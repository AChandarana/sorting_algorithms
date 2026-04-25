package algorithms;
import java.util.Random;
public class BogoSort {
    public static int[] sort(int[] array, int size) {
        while (!isSorted(array, size)) {
            array = randomizeArray(array, size);
        }
        return array;
    }

    private static int[] randomizeArray(int[] array, int size) {
        Random random = new Random();
        int[] takenIndexes = new int[size];
        int[] temp = new int[size];
        for (int i = 0; i < size; ++i) {
            int j = -1;
            while (!doesContain(takenIndexes, j)) {
                j = random.nextInt(size-1);
            }
            array[i] = temp[j];
        }
        return temp;
    }

    private static boolean doesContain(int[] array, int i) {
        if (i==-1) return false;
        for (int j : array) {
            if (i == array[j]) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSorted(int[] array, int size) {
        for (int i = 0; i < size - 1; ++i) {
            if (array[i + 1] < array[i]) return false;
        }
        return true;
    }
}
