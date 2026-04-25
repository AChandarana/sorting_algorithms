import algorithms.BubbleSort;
import java.util.Arrays;
import java.util.Scanner;

public class Driver {
    private static final String[] ALGORITHMS = {
        "Bubble Sort",
        "Bogo Sort"
    };

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Available sorting algorithms:");
        for (int i = 0; i < ALGORITHMS.length; i++) {
            System.out.printf("%d. %s%n", i + 1, ALGORITHMS[i]);
        }

        int algorithmChoice = promptForAlgorithm(scanner);
        int[] values = promptForValues(scanner);
        int[] sortedValues = runAlgorithm(algorithmChoice, values);

        System.out.println("Input:  " + Arrays.toString(values));
        System.out.println("Output: " + Arrays.toString(sortedValues));

        scanner.close();
    }

    private static int promptForAlgorithm(Scanner scanner) {
        while (true) {
            System.out.print("Select an algorithm: ");

            if (!scanner.hasNextInt()) {
                System.out.println("Please enter a number.");
                scanner.next();
                continue;
            }

            int choice = scanner.nextInt();
            scanner.nextLine();

            if (choice >= 1 && choice <= ALGORITHMS.length) {
                return choice;
            }

            System.out.printf("Please enter a number from 1 to %d.%n", ALGORITHMS.length);
        }
    }

    private static int[] promptForValues(Scanner scanner) {
        while (true) {
            System.out.print("Enter integers separated by spaces: ");
            String line = scanner.nextLine().trim();

            if (line.isEmpty()) {
                System.out.println("Please enter at least one integer.");
                continue;
            }

            String[] tokens = line.split("\\s+");
            int[] values = new int[tokens.length];

            try {
                for (int i = 0; i < tokens.length; i++) {
                    values[i] = Integer.parseInt(tokens[i]);
                }
                return values;
            } catch (NumberFormatException exception) {
                System.out.println("Only whole numbers are supported.");
            }
        }
    }

    private static int[] runAlgorithm(int algorithmChoice, int[] values) {
        int[] copy = Arrays.copyOf(values, values.length);

        switch (algorithmChoice) {
            case 1:
                return BubbleSort.sort(copy, copy.length);
            default:
                throw new IllegalArgumentException("Unknown algorithm choice: " + algorithmChoice);
        }
    }
}
