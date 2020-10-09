import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class Driver {

	public static void main(String[] args) throws IOException {

		BTree tree = new BTree("Tree", 60);
//		int nums[] = {9, 5, 1, 13, 17, 2, 6, 7, 8, 3, 4, 10, 18, 11, 12, 14, 19, 15, 16, 20};
//		for(int i = 0 ; i < nums.length; i++) {
//			tree.insert(nums[i], (-1) * nums[i]);
//		}
//		tree.print();


		ArrayList<Integer> vals = new ArrayList<>();
		int v[] = new int[2000];
		Random r = new Random();
		int i = 0;
		while(i < 2000) {
			int num = r.nextInt(1000) + 1;
			if(num!=0 || !vals.contains(num)) {
				vals.add(num);
				v[i] = num;
				tree.insert(num, (-1)*num);
				i++;
			}
		}
//		tree.insert(50, -50);
//		tree.insert(100, -100);
//		tree.insert(150, -150);
//		tree.insert(200, -200);
//		tree.insert(250, -250);
//		tree.insert(300, -300);
//		tree.insert(350, -350);
		tree.print();
//		int l = v.length;
//		for(int j = l - 1; j > 12; j--) {
//			System.out.println("Removing: " + v[j]);
//			tree.remove(v[j]);
//			tree.print();
//		}
		
		i = 0;
		while(i < 2000) {
			tree.remove(v[i]);
			i++;
		}
		
		tree.print();

		
		
		
		
	}

}
