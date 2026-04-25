package algorithms;

public class BubbleSort {

    public static int[] sort(int[] array, int size) {
        for (int pass = 0; pass < size - 1; pass++) {
            for (int i = 0; i < size - pass - 1; i++) {
                if (array[i] > array[i + 1]) {
                    int temp = array[i];
                    array[i] = array[i + 1];
                    array[i + 1] = temp;
                }
            }
        }

        return array;
    }

}
