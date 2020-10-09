import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;
import java.util.Scanner;

public class BTreeTest2 {

	public BTreeTest2() {

	}

	public void test1() throws IOException {
		// this test creates an DBTable with a height 1 order 5 BTree
		// and only does inserts and print
		System.out.println("Start test 1");
		int i;
		int sFieldLens[] = { 10, 15 };
		int nums[] = { 9, 5, 1, 13, 17, 2, 6, 7, 8, 3, 4, 10, 18, 11, 12, 14, 19, 15, 16, 20 };
		int len = nums.length;
		DBTable t1 = new DBTable("t1", sFieldLens, 60);
		char sFields[][] = new char[2][];
		for (i = 0; i < len; i++) {
			sFields[0] = Arrays.copyOf((new Integer(nums[i])).toString().toCharArray(), 10);
			sFields[1] = Arrays.copyOf((new Integer(nums[i])).toString().toCharArray(), 15);

			t1.insert(nums[i], sFields);
		}

		System.out.println("Past inserts in test 1");
		t1.print();

		t1.close();
	}

	public void test2() throws IOException {
		// this test creates an DBTable with a height 1 order 5 BTree
		// and does inserts, print, search and range search
		System.out.println("Start test 2");
		int i;
		int sFieldLens[] = { 10, 15 };
		int nums[] = { 9, 5, 1, 13, 17, 2, 6, 7, 8, 3, 4, 10, 18, 11, 12, 14, 19, 15, 16, 20 };
		int len = nums.length;
		DBTable t2 = new DBTable("t2", sFieldLens, 60);
		char sFields[][] = new char[2][];
		for (i = 0; i < len; i++) {
			sFields[0] = Arrays.copyOf((new Integer(nums[i])).toString().toCharArray(), 10);
			sFields[1] = Arrays.copyOf((new Integer(nums[i])).toString().toCharArray(), 15);

			t2.insert(nums[i], sFields);
		}

		System.out.println("Past inserts in test 2");
		t2.print();
		System.out.println("Past print in test 2");
		System.out.println("Search for 1, 6, 11, 16, 21 in test 2");
		LinkedList<String> s2;
		for (i = 1; i <= 21; i = i + 5) {
			s2 = t2.search(i);
			if (s2.size() == 0)
				System.out.println(i + " not found");
			else
				System.out.println(i + " " + s2.get(0) + " " + s2.get(1));
		}
		System.out.println("Range search 7 to 18 in test 2");
		LinkedList<LinkedList<String>> s2a = t2.rangeSearch(7, 18);
		if (s2a.size() == 0)
			System.out.println("No items found in range 7 to 18");
		else {
			for (int j = 0; j < s2a.size(); j++) {
				s2 = s2a.get(j);
				System.out.println(s2.get(0) + " " + s2.get(1) + " " + s2.get(2));
			}
		}

		t2.close();
	}

	public void test3() throws IOException {
		// this test creates an DBTable with a height 1 order 5 BTree
		// and inserts, removes, print and reuses the tree
		System.out.println("Start test 3");
		int i;
		int sFieldLens[] = { 10, 15 };
		int nums[] = { 9, 5, 1, 13, 17, 2, 6, 7, 8, 3, 4, 10, 18, 11, 12, 14, 19, 15, 16, 20 };
		int len = nums.length;
		DBTable t3 = new DBTable("t3", sFieldLens, 60);
		char sFields[][] = new char[2][];
		for (i = 0; i < len; i++) {
			sFields[0] = Arrays.copyOf((new Integer(nums[i])).toString().toCharArray(), 10);
			sFields[1] = Arrays.copyOf((new Integer(nums[i])).toString().toCharArray(), 15);

			t3.insert(nums[i], sFields);
		}

		System.out.println("Past inserts in test 3");

		for (i = len - 1; i > 4; i--) {
			t3.remove(nums[i]);
		}
		System.out.println("Print after removes in test 3");
		t3.print();

		t3.close();

		t3 = new DBTable("t3");
		System.out.println("Print after reuse in test 3");
		t3.print();
		t3.close();
	}

	public void test4(String s, int nums[], int blockSize, int max) throws IOException {
		// this test inserts and range search with different block sizes
		System.out.println("Start test 4" + s);
		int i;
		int sFieldLens[] = { 10, 15, 25 };
		int len = nums.length;
		DBTable t4 = new DBTable("t4" + s, sFieldLens, blockSize);
		char sFields[][] = new char[3][];
		for (i = 0; i < len; i++) {
			sFields[0] = Arrays.copyOf((new Integer(nums[i])).toString().toCharArray(), 10);
			sFields[1] = Arrays.copyOf((new Integer(nums[i])).toString().toCharArray(), 15);
			sFields[2] = Arrays.copyOf((new Integer(nums[i])).toString().toCharArray(), 25);

			t4.insert(nums[i], sFields);
		}

		System.out.println("Past inserts in test 4" + s);
		System.out.println("Search for all in test4" + s);
		LinkedList<String> s4;
		LinkedList<LinkedList<String>> s4a = t4.rangeSearch(1, max);
		if (s4a.size() == 0)
			System.out.println("No items found in range " + 1 + " " + max);
		else {
			for (int j = 0; j < s4a.size(); j++) {
				s4 = s4a.get(j);
				System.out.println(s4.get(0) + " " + s4.get(1) + " " + s4.get(2) + " " + s4.get(3));
			}
		}
		System.out.println("Search for " + max / 8 + " to " + max / 4 + " in test4" + s);
		s4a = t4.rangeSearch(max / 8, max / 4);
		if (s4a.size() == 0)
			System.out.println("No items found in range " + max / 8 + " " + max / 4);
		else {
			for (int j = 0; j < s4a.size(); j++) {
				s4 = s4a.get(j);
				System.out.println(s4.get(0) + " " + s4.get(1) + " " + s4.get(2) + " " + s4.get(3));
			}
		}
		t4.close();
	}

	public void test5(String s, int nums[], int blockSize) throws IOException {
		// this test insert, removes, reuse and print with different block sizes
		System.out.println("Start test 5" + s);
		int i;
		int sFieldLens[] = { 10, 15, 25 };
		int len = nums.length;
		DBTable t5 = new DBTable("t5" + s, sFieldLens, blockSize);
		char sFields[][] = new char[3][];
		for (i = 0; i < len; i++) {
			sFields[0] = Arrays.copyOf((new Integer(nums[i])).toString().toCharArray(), 10);
			sFields[1] = Arrays.copyOf((new Integer(nums[i])).toString().toCharArray(), 15);
			sFields[2] = Arrays.copyOf((new Integer(nums[i])).toString().toCharArray(), 25);

			t5.insert(nums[i], sFields);
		}

		System.out.println("Past inserts in test 5" + s);
		for (i = len - 1; i > 9; i--) {
			t5.remove(nums[i]);
		}
		System.out.println("Print after removes in test 5" + s);
		t5.print();

		t5.close();

		t5 = new DBTable("t5" + s);
		System.out.println("Print after reuse in test 5" + s);
		t5.print();
		t5.close();
	}

	public void test6(String s, int blockSize) throws IOException {
		// insert 2000 random numbers, remove them all, inserts them again in reverse
		// order and remove all but 12 of them.
		System.out.println("Start test 6" + s);
		int i;
		int nums[] = new int[2000];
		Random r = new Random(2017);
		for (i = 0; i < 2000; i++) {
			nums[i] = r.nextInt();
		}
		int sFieldLens[] = { 15, 15, 20, 30 };
		DBTable t6 = new DBTable("t6" + s, sFieldLens, blockSize);
		char sFields[][] = new char[4][];
		for (i = 0; i < 2000; i++) {
			sFields[0] = Arrays.copyOf((new Integer(nums[i])).toString().toCharArray(), 15);
			sFields[1] = Arrays.copyOf((new Integer(nums[i])).toString().toCharArray(), 15);
			sFields[2] = Arrays.copyOf((new Integer(nums[i])).toString().toCharArray(), 20);
			sFields[3] = Arrays.copyOf((new Integer(nums[i])).toString().toCharArray(), 30);

			t6.insert(nums[i], sFields);
		}
		System.out.println("Test 6" + s + " after inserts");
		for (i = 0; i < 2000; i++) {
			t6.remove(nums[i]);
		}
		System.out.println("Test 6" + s + " after removes");
		for (i = 1999; i >= 0; i--) {
			sFields[0] = Arrays.copyOf((new Integer(nums[i])).toString().toCharArray(), 15);
			sFields[1] = Arrays.copyOf((new Integer(nums[i])).toString().toCharArray(), 15);
			sFields[2] = Arrays.copyOf((new Integer(nums[i])).toString().toCharArray(), 20);
			sFields[3] = Arrays.copyOf((new Integer(nums[i])).toString().toCharArray(), 30);

			t6.insert(nums[i], sFields);
		}
		System.out.println("Test 6" + s + " after second inserts");
		for (i = 1999; i > 11; i--) {
			t6.remove(nums[i]);
		}
		System.out.println("Test 6" + s + " after second removes");
		t6.close();
		t6 = new DBTable("t6" + s);
		t6.print();
		t6.close();

	}

	public static void main(String args[]) throws IOException {
		BTreeTest2 test = new BTreeTest2();
		test.test1();
		test.test2();
		test.test3();

		Scanner scan = new Scanner(System.in);
		System.out.print("Enter the maximum number of keys to use for tests 4 and 5: ");
		int max = scan.nextInt();
		int nums[] = new int[max];
		int start, increment, i;
		int j = 0;
		int divisor = 2;
		while (j < max) {
			start = max / divisor;
			increment = 2 * start;
			for (i = start; i < max && j < max; i = i + increment) {
				nums[j] = i;
				j++;
			}
			divisor = divisor * 2;
		}

		test.test4("a", nums, 72, max);
		test.test4("b", nums, 132, max);
		test.test5("a", nums, 72);
		test.test5("b", nums, 132);

		test.test6("a", 132);
		test.test6("b", 144);
		
		scan.close();

	}

}
