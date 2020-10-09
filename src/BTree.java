import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;
import java.util.Stack;

/**
 * @author AJ Thut
 * @Date 12/6/2019 BTree Implementation
 */
public class BTree {
	private RandomAccessFile f;
	private int order;
	private int blockSize;
	private long root;
	private long free;
	private Stack<BTreeNode> path;
	// variables for the lowest path and new address in insert
	private int smallestval;
	private long loc;
	//linked list for range search method
	private LinkedList<Long> l = new LinkedList<>();

	// add instance variables as needed.
	private class BTreeNode {
		private int count;
		private int keys[];
		private long children[];
		private long address; // the address of the node in the file
		// constructors and other method

		public BTreeNode(long addr) throws IOException {
			f.seek(addr);
			count = f.readInt();
			keys = new int[order];
			for (int i = 0; i < keys.length - 1; i++) {
				keys[i] = f.readInt();
			}

			children = new long[order + 1];
			for (int i = 0; i < children.length - 1; i++) {
				children[i] = f.readLong();
			}

			address = addr;
		}

		public BTreeNode(int keys[], long children[], int count) throws IOException {
			this.keys = keys;
			this.children = children;
			this.count = count;
			address = getFree();
		}

		private void writeNode() throws IOException {
			f.seek(address);
			f.writeInt(count);
			for (int i = 0; i < keys.length - 1; i++) {
				f.writeInt(keys[i]);
			}
			for (int i = 0; i < children.length - 1; i++) {
				f.writeLong(children[i]);
			}
		}
	}

	/**
	 * Private class to represent a free list node in the file
	 */
	private class FreeNode {
		// next freelist node in the file
		private long next;

		public FreeNode(long next) {
			this.next = next;
		}

		/*
		 * Seek to the paraemter and write the next free list node to the file
		 */
		private void writeFree(long addr) throws IOException {
			f.seek(addr);
			f.writeLong(next);
		}
	}

	public BTree(String filename, int bsize) throws IOException {
		// bsize is the block size. This value is used to calculate the order
		// of the B+Tree
		// all B+Tree nodes will use bsize bytes
		// makes a new B+tree
		File p = new File(filename);
		if (p.exists()) {
			p.delete();
		}

		f = new RandomAccessFile(filename, "rw");
		root = 0;
		free = 0;
		blockSize = bsize;
		order = blockSize / 12;

		f.writeLong(root);
		f.writeLong(free);
		f.writeInt(blockSize);
		smallestval = 0;
		loc = 0;
		path = new Stack<>();
	}

	public BTree(String filename) throws IOException {
		// open an existing B+Tree
		f = new RandomAccessFile(filename, "rw");
		f.seek(0);
		root = f.readLong();
		free = f.readLong();
		blockSize = f.readInt();
		path = new Stack<>();
		smallestval = 0;
		loc = 0;
		order = blockSize / 12;
	}

	public boolean insert(int key, long addr) throws IOException {
		/*
		 * If key is not a duplicate add, key to the B+tree addr (in DBTable) is the
		 * address of the row that contains the key. return true if the key is added
		 * return false if the key is a duplicate
		 */
		path.clear();
		BTreeNode node;
		boolean split = false;
		// create new root if tree is empty
		if (root == 0) {
			int[] k = new int[order];
			k[0] = key;
			long c[] = new long[order + 1];
			c[0] = addr;
			node = new BTreeNode(k, c, 1);
			node.count = -1;
			root = node.address;
			node.writeNode();
			return true;
		}

		// search for the key value and return false if its found in the tree
		if (search(key) != 0) {
			return false;
		}

		node = path.pop();
		// if theres room in the node, insert it without spliting
		if (Math.abs(node.count) < order - 1) {
			// put addr and key into their arrays
			node.keys[Math.abs(node.count)] = key;
			node.children[Math.abs(node.count) + 1] = node.children[Math.abs(node.count)];
			node.children[Math.abs(node.count)] = addr;

			// shift each key and child over until key and child is in final spot for the
			// node
			int i = Math.abs(node.count);
			while (i > 0 && node.keys[i] < node.keys[i - 1]) {
				int temp = node.keys[i - 1];
				node.keys[i - 1] = node.keys[i];
				node.keys[i] = temp;

				long temp2 = node.children[i - 1];
				node.children[i - 1] = node.children[i];
				node.children[i] = temp2;

				i--;

			}

			// decrease (increase) the count
			node.count--;

			// write the node out to the file and set split to false
			node.writeNode();
			split = false;
			// clear the stack for the next insert
			path.clear();
			// inserting a value and spliting the node
		} else {
			// spliting a leaf node
			if (node.count < 0) {

				BTreeNode newnode;

				node.keys[Math.abs(node.count)] = key;

				node.children[Math.abs(node.count) + 1] = addr;
				long swap = node.children[Math.abs(node.count) + 1];
				node.children[Math.abs(node.count) + 1] = node.children[Math.abs(node.count)];
				node.children[Math.abs(node.count)] = swap;
				int i = Math.abs(node.count);
				while (i > 0) {
					if (key < node.keys[i - 1]) {
						int temp = node.keys[i - 1];
						node.keys[i - 1] = node.keys[i];
						node.keys[i] = temp;

						long temp2 = node.children[i - 1];
						node.children[i - 1] = node.children[i];
						node.children[i] = temp2;
					}
					i--;
				}

				node.count--;

				// creating the new node and putting in values
				long pointer = node.children[node.children.length - 1];
				int mid = order / 2;
				int[] arrkey = new int[order];
				long[] arrchild = new long[order + 1];
				int index = 0;

				for (int j = mid; j < node.keys.length; j++) {
					arrkey[index] = node.keys[j];
					node.keys[j] = 0;

					arrchild[index] = node.children[j];
					node.children[j] = 0;
					index++;
					node.count++;
				}

				newnode = new BTreeNode(arrkey, arrchild, -1 * index);
				newnode.children[Math.abs(newnode.count)] = pointer;
				node.children[Math.abs(node.count)] = newnode.address;

				node.writeNode();
				newnode.writeNode();
				smallestval = newnode.keys[0];
				loc = newnode.address;
			}
			split = true;
		}

		// while stack is empty and split is true
		while (!path.isEmpty() && split) {
			// pop off next node
			node = path.pop();
			// if there is room, enter smallestval and loc into the node and don't split the
			// node
			if (Math.abs(node.count) < order - 1) {
				if (smallestval < node.keys[0]) {
					// move over all keys to the right one index
					for (int i = node.count; i > 0; i--) {
						node.keys[i] = node.keys[i - 1];
					}

					// put smallestval at 0
					node.keys[0] = smallestval;

					// move over all children to the right one index
					for (int j = node.count + 1; j > 0; j--) {
						node.children[j] = node.children[j - 1];
					}

					// put loc at 0
					node.children[1] = loc;

					// if smallestval is larger than largest value in the node
				} else if (smallestval > node.keys[Math.abs(node.count) - 1]) {
					// put smallestval and loc at last index in each array
					node.keys[Math.abs(node.count)] = smallestval;
					node.children[Math.abs(node.count) + 1] = loc;

					// if smallestval goes somewhere other than the ends of the node
				} else {

					node.children[Math.abs(node.count) + 1] = loc;
					node.keys[Math.abs(node.count)] = smallestval;

					int i = Math.abs(node.count);
					while (smallestval < node.keys[i - 1]) {
						int temp = node.keys[i - 1];
						node.keys[i - 1] = node.keys[i];
						node.keys[i] = temp;

						long temp2 = node.children[i + 1];
						node.children[i + 1] = node.children[i];
						node.children[i] = temp2;
						i--;
					}
				}
				node.count++;
				node.writeNode();

				split = false;
				// clear the stack for the next insert
				path.clear();
				// otherwise we need to split the node and make a new root
			} else {
				// create new node for the split
				BTreeNode newNode = new BTreeNode(new int[order], new long[order + 1], 0);

				if (smallestval < node.keys[0]) {
					// move over all keys to the right one index
					for (int i = node.count; i > 0; i--) {
						node.keys[i] = node.keys[i - 1];
					}

					// put smallestval at 0
					node.keys[0] = smallestval;

					// move over all children to the right one index
					for (int j = node.count + 1; j > 0; j--) {
						node.children[j] = node.children[j - 1];
					}

					// put loc at 0
					node.children[1] = loc;

					// if smallestval is larger than largest value in the node
				} else if (smallestval > node.keys[Math.abs(node.count) - 1]) {
					// put smallestval and loc at last index in each array
					node.keys[Math.abs(node.count)] = smallestval;
					node.children[Math.abs(node.count) + 1] = loc;

					// if smallestval goes somewhere other than the ends of the node
				} else {
					node.children[Math.abs(node.count) + 1] = loc;
					node.keys[Math.abs(node.count)] = smallestval;

					int i = Math.abs(node.count);
					while (smallestval < node.keys[i - 1]) {
						int temp = node.keys[i - 1];
						node.keys[i - 1] = node.keys[i];
						node.keys[i] = temp;

						long temp2 = node.children[i + 1];
						node.children[i + 1] = node.children[i];
						node.children[i] = temp2;
						i--;
					}
				}

				node.count++;

				int middle = order / 2;
				int newval = node.keys[middle];

				// set key values starting at the middle index over to the new node, set node
				// index to 0
				int index = 0;
				// int count = 1;
				while (index < (Math.ceil(order / 2.0) - 1)) {
					newNode.keys[index] = node.keys[middle + 1];
					node.keys[middle + 1] = 0;

					newNode.children[index] = node.children[middle + 1];
					node.children[middle + 1] = 0;

					node.count--;
					newNode.count++;

					middle++;
					index++;
				}

				node.keys[order / 2] = 0;
				node.keys[middle] = 0;

				newNode.children[index] = node.children[middle + 1];
				node.children[middle + 1] = 0;

				node.count--;

				// write the nodes out to the file
				node.writeNode();
				newNode.writeNode();
				// setting loc and smallest val to address of the newnode and smalest value in
				// newnode
				loc = newNode.address;
				smallestval = newval;
				split = true;
			}
		}

		// if split is still true
		if (split) {
			// creat a new root
			BTreeNode newroot = new BTreeNode(new int[order], new long[order + 1], 1);
			// set value in the root to the smallestval
			newroot.keys[0] = smallestval;
			// set the two children of the new root
			newroot.children[0] = root;
			newroot.children[1] = loc;
			// write the node out to the file
			newroot.writeNode();
			root = newroot.address;
			// write out the new root to the file
			f.seek(0);
			f.writeLong(root);
			// clear the stack for the next insert
			path.clear();
		}

		return true;

	}

	public long remove(int key) throws IOException {
		/*
		 * If the key is in the Btree, remove the key and return the address of the row
		 * return 0 if the key is not found in the B+tree
		 */
		if (root == 0) {
			return 0;
		}
		BTreeNode node;
		boolean toosmall = false;
		long returnaddr = 0;
		search(key);
		node = path.pop();

		int val = inNode(node, key);
		// if the key is in the node
		if (val != -1) {
			// shifting over key values to the left
			returnaddr = node.children[val];
			for (int i = val; i < Math.abs(node.count); i++) {
				node.keys[i] = node.keys[i + 1];
			}

			// shitfting over children to the left
			for (int i = val; i < Math.abs(node.count) + 1; i++) {
				node.children[i] = node.children[i + 1];
			}
			// increase count because it is negative
			node.count++;
			node.writeNode();
			// if we don't have enough values, set tosmall to true
			if (Math.abs(node.count) < (int) Math.ceil(order / 2.0) - 1) {
				toosmall = true;
				if (node.address == root) {
					toosmall = false;
				}
			}
			node.writeNode();
			if (node.address == root && node.count == 0) {
				for (int i = 0; i < Math.abs(node.count); i++) {
					node.keys[i] = 0;
					node.children[i] = 0;
				}
				node.writeNode();
				addFree(root);
				root = 0;
				f.seek(0);
				f.writeLong(free);
				// lowestkeys.remove(node.address);
				return returnaddr;
			}
		} else {
			return 0;
		}

		while (!path.isEmpty() && toosmall) {
			BTreeNode child = node;
			// set node to the parent of child
			node = path.pop();
			// childaddr = index of the child index in the parent node
			int childaddr = 0;
			for (int i = 0; i < node.children.length; i++) {
				if (node.children[i] == child.address) {
					childaddr = i;
					break;
				}
			}
			// speical case if the child value is the right most leaf from the parent
			if (childaddr == Math.abs(node.count)) {
				// create a node at the leaf to the left of the child
				BTreeNode neighbor = new BTreeNode(node.children[childaddr - 1]);
				if (Math.abs(neighbor.count) > (int) Math.ceil(order / 2.0) - 1) {

					// if we can borrow, root is parent, and not in a leaf
					if (node.address == root && node.count == 1 && child.count > 0) {
						for (int i = Math.abs(child.count); i > 0; i--) {
							child.keys[i] = child.keys[i - 1];
						}
						for (int i = Math.abs(child.count) + 1; i > 0; i--) {
							child.children[i] = child.children[i - 1];
						}
						// putting largest val of neighbor into root and child
						child.keys[0] = node.keys[0];
						node.keys[0] = neighbor.keys[Math.abs(neighbor.count) - 1];
						child.children[0] = neighbor.children[Math.abs(neighbor.count)];

						neighbor.children[Math.abs(neighbor.count)] = 0;
						neighbor.keys[Math.abs(neighbor.count) - 1] = 0;

						child.count++;
						neighbor.count--;

						child.writeNode();
						node.writeNode();
						neighbor.writeNode();
						toosmall = false;
						// if we can borrow but in a leaf but parent is the root
					} else if (node.address == root && node.count == 1 && child.count < 0) {
						for (int i = Math.abs(child.count); i > 0; i--) {
							child.keys[i] = child.keys[i - 1];
							child.children[i] = child.children[i - 1];
						}

						child.keys[0] = neighbor.keys[Math.abs(neighbor.count) - 1];
						neighbor.keys[Math.abs(neighbor.count) - 1] = 0;
						child.children[0] = neighbor.children[Math.abs(neighbor.count) - 1];
						neighbor.children[Math.abs(neighbor.count) - 1] = neighbor.children[Math.abs(neighbor.count)];
						neighbor.children[Math.abs(neighbor.count)] = 0;

						child.count--;
						node.keys[0] = child.keys[0];

						neighbor.count++;
						neighbor.writeNode();
						child.writeNode();
						node.writeNode();
						toosmall = false;
					} else {
						//borrowing from a child node
						if (child.count < 0) {
							child.children[Math.abs(child.count) + 1] = child.children[Math.abs(child.count)];
							for (int i = Math.abs(child.count); i > 0; i--) {
								child.keys[i] = child.keys[i - 1];
								child.children[i] = child.children[i - 1];
							}
							child.keys[0] = neighbor.keys[Math.abs(neighbor.count) - 1];
							neighbor.keys[Math.abs(neighbor.count) - 1] = 0;
							child.children[0] = neighbor.children[Math.abs(neighbor.count) - 1];
							neighbor.children[Math.abs(neighbor.count) - 1] = neighbor.children[Math
									.abs(neighbor.count)];
							neighbor.children[Math.abs(neighbor.count)] = 0;
							// put new smallest value of the child node into parent node at the correct
							// position
							node.keys[childaddr - 1] = child.keys[0];

							child.count--;
							neighbor.count++;
							node.writeNode();
							child.writeNode();
							neighbor.writeNode();
							toosmall = false;
							//borrowing from a non leaf node
						} else {
							for (int i = child.count; i > 0; i--) {
								child.keys[i] = child.keys[i - 1];
							}
							for (int i = child.count + 1; i > 0; i--) {
								child.children[i] = child.children[i - 1];
							}

							child.keys[0] = node.keys[childaddr - 1];
							child.children[0] = neighbor.children[Math.abs(neighbor.count)];
							node.keys[childaddr - 1] = neighbor.keys[Math.abs(neighbor.count) - 1];

							neighbor.keys[Math.abs(neighbor.count) - 1] = 0;
							neighbor.children[Math.abs(neighbor.count)] = 0;

							child.count++;
							neighbor.count--;
							node.writeNode();
							child.writeNode();
							neighbor.writeNode();
							toosmall = false;
						}

					}
				} else {
					//combining a node if root is parent
					if (node.address == root && node.count == 1 && child.count > 0) {
						neighbor.keys[Math.abs(neighbor.count)] = node.keys[0];
						neighbor.count++;
						int childstart = Math.abs(neighbor.count);
						int idx = Math.abs(neighbor.count);
						for (int i = 0; i < Math.abs(child.count); i++) {
							neighbor.keys[idx] = child.keys[i];
							neighbor.count++;
							idx++;
						}
						for (int i = 0; i < Math.abs(child.count) + 1; i++) {
							neighbor.children[childstart] = child.children[i];
							childstart++;
						}
						node.keys[0] = 0;
						node.children[1] = 0;
						neighbor.writeNode();
						node.writeNode();
						toosmall = true;
						//combining node if root is parent but in a leaf node
					} else if (node.address == root && node.count == 1 && child.count < 0) {

						int index = Math.abs(neighbor.count);
						for (int i = 0; i < Math.abs(child.count); i++) {
							neighbor.keys[index] = child.keys[i];
							neighbor.children[index] = child.children[i];
							neighbor.count--;
							index++;
						}

						toosmall = true;
						node.keys[0] = 0;
						node.children[1] = 0;
						neighbor.writeNode();
						node.writeNode();
						//combining in a leaf node or non leaf node without root as the parent
					} else {
						if (child.count < 0) {
							int index = Math.abs(neighbor.count);
							neighbor.children[Math.abs(neighbor.count) + Math.abs(child.count)] = child.children[Math
									.abs(child.count)];

							for (int i = 0; i < Math.abs(child.count); i++) {
								neighbor.keys[index] = child.keys[i];
								neighbor.children[index] = child.children[i];
								neighbor.count--;
								index++;
							}
							neighbor.children[Math.abs(neighbor.count)] = child.children[Math.abs(child.count)];
							node.children[childaddr] = 0;
							node.keys[childaddr - 1] = 0;
							node.count--;

							if (node.address == root && node.count == 1) {
								toosmall = false;
							}
							if (node.count >= (int) Math.ceil(order / 2.0) - 1) {
								toosmall = false;
							}
							if (node.address == root && node.count <= (int) Math.ceil(order / 2.0) - 1) {
								toosmall = false;
							}

							neighbor.writeNode();
							node.writeNode();
							addFree(child.address);
						} else {
							neighbor.keys[Math.abs(neighbor.count)] = node.keys[childaddr - 1];
							neighbor.count++;
							int index = Math.abs(neighbor.count);
							int count = 0;
							for (int i = 0; i < child.count; i++) {
								neighbor.keys[index] = child.keys[i];
								index++;
								count++;
							}
							index = Math.abs(neighbor.count);
							for (int i = 0; i < child.count + 1; i++) {
								neighbor.children[index] = child.children[i];
								index++;
							}
							neighbor.count += count;
							node.children[childaddr] = 0;
							node.keys[childaddr - 1] = 0;
							node.count--;

							if (node.address == root && node.count == 1) {
								toosmall = false;
							} else if (node.count >= (int) Math.ceil(order / 2.0) - 1) {
								toosmall = false;
							} else if (node.address == root && node.count <= (int) Math.ceil(order / 2.0) - 1) {
								toosmall = false;
							}

							neighbor.writeNode();
							node.writeNode();
							addFree(child.address);
						}
					}
				}
				// make neighbor node if not at final child and do rotations
			} else {
				BTreeNode neighbor = new BTreeNode(node.children[childaddr + 1]);
				if (Math.abs(neighbor.count) > (int) Math.ceil(order / 2.0) - 1) {

					// if we need to borrow from neighbor but not in a leaf
					if (node.address == root && node.count == 1 && child.count > 0) {
						child.keys[Math.abs(child.count)] = node.keys[0];
						node.keys[0] = neighbor.keys[0];
						child.children[Math.abs(child.count) + 1] = neighbor.children[0];

						// shifting over all of the keys and children in the neighbor node
						for (int i = 0; i < Math.abs(neighbor.count); i++) {
							neighbor.keys[i] = neighbor.keys[i + 1];
						}
						for (int i = 0; i < Math.abs(neighbor.count); i++) {
							neighbor.children[i] = neighbor.children[i + 1];
						}

						neighbor.keys[Math.abs(neighbor.count)] = 0;

						child.count++;
						neighbor.count--;

						child.writeNode();
						node.writeNode();
						neighbor.writeNode();
						toosmall = false;
						// if we can borrow but in a leaf but parent is the root
					} else if (node.address == root && node.count == 1 && child.count < 0) {
						// int ind = inNode(neighbor, node.keys[0]);
						// if (ind == -1) {
						child.keys[Math.abs(child.count)] = neighbor.keys[0];
						child.children[Math.abs(child.count) + 1] = child.children[Math.abs(child.count)];
						child.children[Math.abs(child.count)] = neighbor.children[0];
						child.count--;
						node.keys[0] = neighbor.keys[1];
						for (int i = 0; i < Math.abs(neighbor.count); i++) {
							neighbor.keys[i] = neighbor.keys[i + 1];
							neighbor.children[i] = neighbor.children[i + 1];
						}
						neighbor.count++;
						neighbor.writeNode();
						child.writeNode();
						node.writeNode();
						toosmall = false;
						// }

					} else {

						if (child.count < 0) {
							// set end of child node to the new value and send new smallest value of
							// neighbor to the root
							child.keys[Math.abs(child.count)] = neighbor.keys[0];
							child.children[Math.abs(child.count) + 1] = child.children[Math.abs(child.count)];
							child.children[Math.abs(child.count)] = neighbor.children[0];
							// putting new lowest value of the neighbor into the parent
							node.keys[childaddr] = neighbor.keys[1];

							for (int i = 0; i < Math.abs(neighbor.count); i++) {
								neighbor.keys[i] = neighbor.keys[i + 1];
								neighbor.children[i] = neighbor.children[i + 1];
							}

							neighbor.children[Math.abs(neighbor.count)] = 0;
							// increase/decrease the counts
							child.count--;
							neighbor.count++;
							node.writeNode();
							child.writeNode();
							neighbor.writeNode();
							toosmall = false;
						} else {

							child.keys[Math.abs(child.count)] = node.keys[childaddr];
							child.children[Math.abs(child.count) + 1] = neighbor.children[0];
							node.keys[childaddr] = neighbor.keys[0];
							// shitfting over neighbor keys and children
							for (int i = 0; i < Math.abs(neighbor.count); i++) {
								neighbor.keys[i] = neighbor.keys[i + 1];
							}
							neighbor.keys[Math.abs(neighbor.count)] = 0;
							for (int i = 0; i < Math.abs(neighbor.count) + 1; i++) {
								neighbor.children[i] = neighbor.children[i + 1];
							}
							neighbor.children[Math.abs(neighbor.count) + 1] = 0;

							child.count++;
							neighbor.count--;
							node.writeNode();
							child.writeNode();
							neighbor.writeNode();
							toosmall = false;
						}
					}
				} else {

					if (node.address == root && node.count == 1 && child.count > 0) {
						child.keys[Math.abs(child.count)] = node.keys[0];
						child.count++;
						int childstart = Math.abs(child.count);
						int idx = Math.abs(child.count);
						for (int i = 0; i < Math.abs(neighbor.count); i++) {
							child.keys[idx] = neighbor.keys[i];
							child.count++;
							idx++;
						}
						for (int i = 0; i < Math.abs(neighbor.count) + 1; i++) {
							child.children[childstart] = neighbor.children[i];
							childstart++;
						}
						node.keys[0] = 0;
						node.children[1] = 0;
						child.writeNode();
						node.writeNode();
						toosmall = true;
					} else if (node.address == root && node.count == 1 && child.count < 0) {
						child.children[Math.abs(child.count) + Math.abs(neighbor.count)] = neighbor.children[Math
								.abs(neighbor.count)];

						int index = Math.abs(child.count);
						for (int i = 0; i < Math.abs(neighbor.count); i++) {
							child.keys[index] = neighbor.keys[i];
							child.children[index] = neighbor.children[i];
							child.count--;
							index++;
						}

						toosmall = true;
						node.keys[0] = 0;
						node.children[1] = 0;
						child.writeNode();
						node.writeNode();
						// lowestkeys.remove(neighbor.address);

					} else {
						if (child.count < 0) {
							int index = Math.abs(child.count);

							// put pointer of neighbor node into the child node pointer but shifted over
							child.children[Math.abs(child.count) + Math.abs(neighbor.count)] = neighbor.children[Math
									.abs(neighbor.count)];

							// bring over all neighbor's children and keys
							for (int i = 0; i < Math.abs(neighbor.count); i++) {
								child.keys[index] = neighbor.keys[i];
								child.children[index] = neighbor.children[i];
								child.count--;
								index++;
							}
							// getting rid of the refrence to the neighbor node
							node.children[childaddr + 1] = 0;
							// getting rid of key in the parent node
							node.keys[childaddr] = 0;
							// shitfing over all of the keys in the node
							for (int i = childaddr; i < node.count; i++) {
								node.keys[i] = node.keys[i + 1];
							}
							// shifting over all of the children in the node
							for (int i = childaddr + 1; i < node.count + 1; i++) {
								node.children[i] = node.children[i + 1];
							}
							node.children[node.count + 1] = 0;
							node.count--;

							if (node.address == root && node.count == 1) {
								toosmall = false;
							}
							// if we have enough keys, set tosmall to false
							if (node.count >= (int) Math.ceil(order / 2.0) - 1) {
								toosmall = false;
							}

							if (node.address == root && node.count <= (int) Math.ceil(order / 2.0) - 1) {
								toosmall = false;
							}

							child.writeNode();
							node.writeNode();

							// add address of neighbor to the freelist
							addFree(neighbor.address);
							// lowestkeys.remove(neighbor.address);
						} else {

							child.keys[child.count] = node.keys[childaddr];
							child.count++;

							int index = Math.abs(child.count);
							int count = 0;
							// bring over the neighbor keys
							for (int i = 0; i < neighbor.count; i++) {
								child.keys[index] = neighbor.keys[i];
								index++;
								count++;
							}
							// bring over the neighbor children
							index = Math.abs(child.count);
							for (int i = 0; i < neighbor.count + 1; i++) {
								child.children[index] = neighbor.children[i];
								index++;
							}
							// shifting over the keys and children in the parent node
							child.count += count;
							for (int i = childaddr; i < node.count; i++) {
								node.keys[i] = node.keys[i + 1];
							}
							node.keys[node.count] = 0;
							for (int i = childaddr + 1; i < node.count; i++) {
								node.children[i] = node.children[i + 1];
							}
							node.children[node.count] = 0;
							node.count--;

							node.writeNode();
							child.writeNode();
							addFree(neighbor.address);
							// lowestkeys.remove(neighbor.address);
							if (node.address == root && node.count == 1) {
								toosmall = false;
							} else if (node.count >= (int) Math.ceil(order / 2.0) - 1) {
								toosmall = false;
							} else if (node.address == root && node.count <= (int) Math.ceil(order / 2.0) - 1) {
								toosmall = false;
							}
						}
					}
				}
			}
		}

		//if we brang the root down
		if (toosmall) {
			BTreeNode r = new BTreeNode(root);
			addFree(root);
			root = r.children[0];
			f.seek(0);
			f.writeLong(root);

		}
		//clear the path and return the address on the value that was removed
		path.clear();
		return returnaddr;

	}

	/**
	 * @param node - BTree Node
	 * @param key - key we are searching for
	 * @return address in the dbtable or -1 if not found
	 */
	private int inNode(BTreeNode node, int key) {
		for (int i = 0; i < Math.abs(node.count); i++) {
			// return index of the key
			if (node.keys[i] == key) {
				return i;
			}
		}

		// otherwise return -1
		return -1;
	}

	public long search(int k) throws IOException {
		/*
		 * This is an equality search. If the key is found return the address of the row
		 * with the key otherwise return 0
		 */
		return search(k, root);

	}

	private long search(int k, long addr) throws IOException {
		//create new node and push onto the stack
		BTreeNode node = new BTreeNode(addr);
		path.push(node);
		//check if the node is the root and if the count is 1
		if (node.address == root && node.count == 1) {
			if (k >= node.keys[0]) {
				return search(k, node.children[1]);
			}
			return search(k, node.children[0]);
		} else {
			//find the value and return the address of node if the value if it is in the leaf node
			if (node.count < 0) {
				for (int i = 0; i < Math.abs(node.count); i++) {
					if (node.keys[i] == k) {
						return node.children[i];
					}
				}
				return 0;
				//if not in a leaf node, search for value based on the keys in the non leaf node
			} else {
				int i = 1;
				while (i <= Math.abs(node.count) && k >= node.keys[i - 1]) {
					i++;
				}
				return search(k, node.children[i - 1]);
			}
		}
	}

	public LinkedList<Long> rangeSearch(int low, int high) throws IOException {
		// PRE: low <= high
		/*
		 * return a list of row addresses for all keys in the range low to high
		 * inclusive return an empty list when no keys are in the range
		 */
		l.clear();
		//make new node and go down to left most leaf node in the tree
		BTreeNode x = new BTreeNode(root);
		while (x.count > 0) {
			x = new BTreeNode(x.children[0]);
		}
		//do a range search at that node
		rangeSearch(low, high, x.address);

		return l;
	}

	/**
	 * @param low - lowest val
	 * @param high - highest val
	 * @param addr - address on the node 
	 * @throws IOException
	 */
	private void rangeSearch(int low, int high, long addr) throws IOException {
		if (addr == 0) {
			return;
		}
		//create a node and add its values to linked list if they are within the range
		BTreeNode x = new BTreeNode(addr);
		for (int i = 0; i < Math.abs(x.count); i++) {
			if (x.keys[i] >= low && x.keys[i] <= high) {
				l.add(x.children[i]);
			}
		}
		//go to next leaf and do a range search on its values
		rangeSearch(low, high, x.children[Math.abs(x.count)]);

	}

	public void print() throws IOException {
		// print the B+Tree to standard output
		// print one node per line
		// This method can be helpful for debugging
		print(root);
		System.out.println("\n");
	}

	private void print(long addr) throws IOException {
		if (root == 0) {
			System.out.println("TREE IS EMPTY");
			return;
		}

		if (addr == 0) {
			return;
		}

		BTreeNode node = new BTreeNode(addr);
		//print for a leaf node
		if (node.count < 0) {
			System.out.println("PRINTING LEAF NODE: ");
			System.out.print("KEYS: ");
			for (int i = 0; i < node.keys.length; i++) {
				System.out.print(node.keys[i] + " ");
			}
			System.out.println();
			System.out.print("CHILDREN: ");
			for (int i = 0; i < node.children.length; i++) {
				System.out.print(node.children[i] + " ");
			}

			System.out.println("\n");
			//print for a non leaf node
		} else {
			System.out.println("PRINTING A NON LEAF NODE: ");
			System.out.print("KEYS: ");
			for (int i = 0; i < node.keys.length; i++) {
				System.out.print(node.keys[i] + " ");
			}
			System.out.println();
			System.out.print("CHILDREN: ");
			for (int i = 0; i < node.children.length; i++) {
				System.out.print(node.children[i] + " ");
			}
			System.out.println("\n");
			for (int i = 0; i < node.children.length; i++) {
				print(node.children[i]);
			}
		}
	}

	public void close() throws IOException {
		// close the B+tree. The tree should not be accessed after close is called
		f.close();
	}

	/**
	 * @return long - address on next available position in the file
	 * @throws IOException
	 */
	private long getFree() throws IOException {
		// if nothing is in the freelist, return the length of file
		if (free == 0) {
			return f.length();
		}

		// store address in free, seek to address in free and set that to free
		long addr = free;
		f.seek(free);
		free = f.readLong();

		// seek to free position and write the new free
		f.seek(8);
		f.writeLong(free);

		// return addr
		return addr;
	}

	/**
	 * Adds address to the freelist if it was removed
	 * @param addr
	 * @throws IOException
	 */
	private void addFree(long addr) throws IOException {
		//create free node with the next address, write it out to the file
		FreeNode fr = new FreeNode(free);
		free = addr;
		fr.writeFree(addr);

		f.seek(8);
		f.writeLong(free);
	}
}
