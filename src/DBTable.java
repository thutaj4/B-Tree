import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;

public class DBTable {
	private RandomAccessFile rows; // the file that stores the rows in the table
	private long free; // head of the free list space for rows
	private int numOtherFields;
	private int otherFieldLengths[];
	// add other instance variables as needed
	BTree tree;
	long f;

	private class Row {
		private int keyField;
		private char otherFields[][];

		/*
		 * Each row consists of unique key and one or more character array fields. Each
		 * character array field is a fixed length field (for example 10 characters).
		 * Each field can have a different length. Fields are padded with null
		 * characters so a field with a length of of x characters always uses space for
		 * x characters.
		 */
		// Constructors and other Row methods
		public Row(int key, char fields[][]) {
			keyField = key;
			otherFields = fields;
		}

		public Row(long addr) throws IOException {
			rows.seek(addr);
			keyField = rows.readInt();
			otherFields = new char[numOtherFields][];
			for (int i = 0; i < otherFieldLengths.length; i++) {
				otherFields[i] = new char[otherFieldLengths[i]];
			}

			for (int i = 0; i < otherFields.length; i++) {
				for (int j = 0; j < otherFields[i].length; j++) {
					otherFields[i][j] = rows.readChar();
				}
			}
		}

		private void writeRow(long addr) throws IOException {
			rows.seek(addr);
			rows.writeInt(keyField);
			for (int i = 0; i < otherFields.length; i++) {
				for (int j = 0; j < otherFields[i].length; j++) {
					rows.writeChar(otherFields[i][j]);
				}
			}

		}
	}

	/**
	 * Private class for free list values in the table
	 */
	private class FreeNode {
		// the next address of the next node in the free list
		private long nextfree;

		public FreeNode(long next) {
			nextfree = next;
		}

		/*
		 * Method seeks to the parameter address and writes out nextfree
		 */
		private void writeNode(long addr) throws IOException {
			rows.seek(addr);
			rows.writeLong(nextfree);
		}
	}

	public DBTable(String filename, int fL[], int bsize) throws IOException {
		/*
		 * Use this constructor to create a new DBTable. filename is the name of the
		 * file used to store the table fL is the lengths of the otherFields fL.length
		 * indicates how many other fields are part of the row. bsize is the block size.
		 * It is used to calculate the order of the B+Tree. A B+Tree must be created for
		 * the key field in the table
		 * 
		 * If a file with name filename exists, the file should be deleted before the
		 * new file is created.
		 */

		File path = new File(filename);
		if (path.exists()) {
			path.delete();
		}

		free = 0;
		rows = new RandomAccessFile(filename, "rw");
		otherFieldLengths = fL;
		numOtherFields = fL.length;
		tree = new BTree("BTree", bsize);

		rows.writeInt(numOtherFields);

		for (int i = 0; i < otherFieldLengths.length; i++) {
			rows.writeInt(otherFieldLengths[i]);
		}
		f = rows.getFilePointer();
		rows.writeLong(free);

	}

	public DBTable(String filename) throws IOException {
		// Use this constructor to open an existing DBTable
		rows = new RandomAccessFile(filename, "rw");
		rows.seek(0);
		numOtherFields = rows.readInt();
		otherFieldLengths = new int[numOtherFields];
		for (int i = 0; i < numOtherFields; i++) {
			otherFieldLengths[i] = rows.readInt();

		}

		f = rows.getFilePointer();
		free = rows.readLong();
		tree = new BTree("BTree");

	}

	public boolean insert(int key, char fields[][]) throws IOException {
		// PRE: the length of each row is fields matches the expected length
		/*
		 * If a row with the key is not in the table, the row is added and the method
		 * returns true otherwise the row is not added and the method returns false. The
		 * method must use the B+tree to determine if a row with the key exists. If the
		 * row is added the key is also added into the B+tree.
		 */

		// create a new row
		Row r = new Row(key, fields);
		long ro = getFree();
		// if inserted into the btree, insert into the dtable
		if (tree.insert(key, ro)) {
			r.writeRow(ro);
			return true;
		}

		return false;

	}

	public boolean remove(int key) throws IOException {
		/*
		 * If a row with the key is in the table it is removed and true is returned
		 * otherwise false is returned. The method must use the B+Tree to determine if a
		 * row with the key exists.
		 * 
		 * If the row is deleted the key must be deleted from the B+Tree
		 */

		// get the addres from the tree
		long addr = tree.remove(key);
		if (addr == 0) {
			return false;
		} else {
			// if key was in the tree, add its address to the free list
			addFree(addr);
			return true;
		}

	}

	/**
	 * Adds address to the freelist if it was removed from the table
	 * 
	 * @param addr - address to add
	 * @throws IOException
	 */
	private void addFree(long addr) throws IOException {
		if (free == 0) {
			free = addr;
			rows.seek(free);
			rows.writeLong(0);
		} else {
			// create a free node at free
			FreeNode freenode = new FreeNode(free);
			free = addr;
			freenode.writeNode(addr);

			// seek to free and write it out to the file
			rows.seek(f);
			rows.writeLong(free);
		}
	}

	/**
	 * Gets the next available position in the file to create a row
	 * 
	 * @return - address in the file
	 * @throws IOException
	 */
	private long getFree() throws IOException {
		// if nothing is in the freelist, return the length of file
		if (free == 0) {
			return rows.length();
		}

		// store address in free, seek to address in free and set that to free
		long addr = free;
		rows.seek(free);
		free = rows.readLong();

		// seek to free position and write the new free
		rows.seek(f);
		rows.writeLong(free);

		// return addr
		return addr;
	}

	public LinkedList<String> search(int key) throws IOException {
		/*
		 * If a row with the key is found in the table return a list of the other fields
		 * in the row. The string values in the list should not include the null
		 * characters. If a row with the key is not found return an empty list. The
		 * method must use the equality search in B+Tree
		 */
		// create linked list to return and get address of the key in the tree
		LinkedList<String> list = new LinkedList<>();
		long result = tree.search(key);

		if (result == 0) {
			return list;
		}
		// create row at the result
		Row r = new Row(result);
		// add the otherfields to the linked list
		for (int i = 0; i < r.otherFields.length; i++) {
			String s = "";
			for (int j = 0; j < r.otherFields[i].length; j++) {
				if (r.otherFields[i][j] == '\0') {
					break;
				}
				s += Character.toString(r.otherFields[i][j]);
			}
			list.add(s);
		}

		return list;

	}

	public LinkedList<LinkedList<String>> rangeSearch(int low, int high) throws IOException {
		// PRE: low <= high
		/*
		 * For each row with a key that is in the range low to high inclusive a list of
		 * the fields (including the key) in the row is added to the list returned by
		 * the call. If there are no rows with a key in the range return an empty list
		 * The method must use the range search in B+Tree
		 */

		// create linked list of linked lists to return and get a linked list of the
		// range serach method
		LinkedList<LinkedList<String>> list = new LinkedList<LinkedList<String>>();
		LinkedList<Long> addrs = tree.rangeSearch(low, high);

		for (int i = 0; i < addrs.size(); i++) {
			// add each row to a linked list
			Row r = new Row(addrs.get(i));
			LinkedList<String> temp = new LinkedList<>();
			temp.add(Integer.toString(r.keyField));
			for (int a = 0; a < r.otherFields.length; a++) {
				String s = "";
				for (int j = 0; j < r.otherFields[a].length; j++) {
					if (r.otherFields[a][j] == '\0') {
						break;
					}
					s += Character.toString(r.otherFields[a][j]);
				}
				temp.add(s);
			}
			// add new linked list to the list to return
			list.add(temp);
		}

		return list;

	}

	public void print() throws IOException {
		// Print the rows to standard output is ascending order (based on the keys)
		// One row per line

		LinkedList<Long> list = tree.rangeSearch(Integer.MIN_VALUE, Integer.MAX_VALUE);

		for (int i = 0; i < list.size(); i++) {
			Row r = new Row(list.get(i));
			// System.out.println(r.keyField);
			for (int j = 0; j < r.otherFields.length; j++) {
				for (int k = 0; k < r.otherFields[j].length; k++) {
					if (r.otherFields[j][k] == '\0') {
						break;
					}
					System.out.print(r.otherFields[j][k]);
				}
				System.out.print(" ");
			}
			System.out.println();
		}
	}

	public void close() throws IOException {
		// close the DBTable. The table should not be used after it is closed
		rows.close();
	}
}
