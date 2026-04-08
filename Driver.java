import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

interface SortAlgorithm {
    String name();

    void sort(int[] array, SortStats stats);
}

final class SortStats {
    private long comparisons;
    private long swaps;
    private long writes;

    public int compare(int left, int right) {
        comparisons++;
        return Integer.compare(left, right);
    }

    public boolean lessThan(int left, int right) {
        return compare(left, right) < 0;
    }

    public boolean lessThanOrEqual(int left, int right) {
        return compare(left, right) <= 0;
    }

    public boolean greaterThan(int left, int right) {
        return compare(left, right) > 0;
    }

    public boolean greaterThanOrEqual(int left, int right) {
        return compare(left, right) >= 0;
    }

    public boolean equalTo(int left, int right) {
        return compare(left, right) == 0;
    }

    public void swap(int[] array, int firstIndex, int secondIndex) {
        if (firstIndex == secondIndex) {
            return;
        }

        int temp = array[firstIndex];
        array[firstIndex] = array[secondIndex];
        array[secondIndex] = temp;
        swaps++;
        writes += 2;
    }

    public void write(int[] array, int index, int value) {
        array[index] = value;
        writes++;
    }

    public void recordComparison() {
        comparisons++;
    }

    public void recordSwap() {
        swaps++;
    }

    public void recordWrite() {
        writes++;
    }

    public long getComparisons() {
        return comparisons;
    }

    public long getSwaps() {
        return swaps;
    }

    public long getWrites() {
        return writes;
    }
}

public class Driver {
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^\\s*package\\s+([a-zA-Z_][\\w.]*)\\s*;",
            Pattern.MULTILINE);
    private static final Pattern CLASS_PATTERN = Pattern.compile("\\bclass\\s+([a-zA-Z_][\\w]*)\\b");
    private static final int PREVIEW_LENGTH = 12;

    public static void main(String[] args) {
        List<AlgorithmEntry> algorithms = discoverAlgorithms();

        if (algorithms.isEmpty()) {
            System.out.println("No runnable sorting algorithms were found.");
            System.out.println();
            System.out.println("Supported options:");
            System.out.println("1. Implement SortAlgorithm in another .java file.");
            System.out.println("2. Or add a sort method with one of these signatures:");
            System.out.println("   void sort(int[] array, SortStats stats)");
            System.out.println("   static void sort(int[] array, SortStats stats)");
            System.out.println("   void sort(int[] array)");
            System.out.println("   static void sort(int[] array)");
            return;
        }

        try (Scanner scanner = new Scanner(System.in)) {
            printMenu(algorithms);

            AlgorithmEntry selected = algorithms.get(
                    readIntInRange(scanner, "Choose an algorithm by number: ", 1, algorithms.size()) - 1);

            int size = readIntInRange(scanner, "How many numbers should be sorted? ", 1, Integer.MAX_VALUE);
            int maxValue = readIntInRange(
                    scanner,
                    "Largest random value to generate? ",
                    0,
                    Integer.MAX_VALUE);
            long seed = readLong(scanner, "Random seed (enter any whole number): ");

            int[] original = generateRandomArray(size, maxValue, seed);
            int[] workingCopy = original.clone();
            SortStats stats = new SortStats();

            long startedAt = System.nanoTime();
            selected.runner.run(workingCopy, stats);
            long elapsedNanos = System.nanoTime() - startedAt;

            boolean sorted = isSorted(workingCopy);

            System.out.println();
            System.out.println("Algorithm: " + selected.displayName);
            System.out.println("Source: " + selected.sourcePath);
            System.out.println("Input size: " + size);
            System.out.println("Random seed: " + seed);
            System.out.println("Original sample: " + previewArray(original));
            System.out.println("Sorted sample: " + previewArray(workingCopy));
            System.out.println("Sorted correctly: " + (sorted ? "yes" : "no"));
            System.out.println("Comparisons: " + stats.getComparisons());
            System.out.println("Swaps: " + stats.getSwaps());
            System.out.println("Writes: " + stats.getWrites());
            System.out.printf("Elapsed time: %.3f ms%n", elapsedNanos / 1_000_000.0);

            if (!sorted) {
                System.out.println();
                System.out.println("The result is not sorted. Check the algorithm implementation.");
            }
        } catch (ReflectiveOperationException exception) {
            System.out.println("Failed to run the selected algorithm: " + exception.getMessage());
        }
    }

    private static void printMenu(List<AlgorithmEntry> algorithms) {
        System.out.println("Available sorting algorithms:");
        for (int index = 0; index < algorithms.size(); index++) {
            AlgorithmEntry entry = algorithms.get(index);
            System.out.printf("%d. %s [%s]%n", index + 1, entry.displayName, entry.sourcePath);
        }
        System.out.println();
    }

    private static List<AlgorithmEntry> discoverAlgorithms() {
        List<AlgorithmEntry> entries = new ArrayList<>();
        Path root = Paths.get("").toAbsolutePath().normalize();

        try (java.util.stream.Stream<Path> paths = Files.walk(root)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.getFileName().toString().equals("Driver.java"))
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(path -> buildEntry(path, root).ifPresent(entries::add));
        } catch (IOException exception) {
            System.out.println("Unable to scan Java files: " + exception.getMessage());
        }

        entries.sort(Comparator.comparing(entry -> entry.displayName.toLowerCase()));
        return entries;
    }

    private static Optional<AlgorithmEntry> buildEntry(Path sourcePath, Path root) {
        try {
            String source = new String(Files.readAllBytes(sourcePath), StandardCharsets.UTF_8);
            String packageName = matchFirstGroup(PACKAGE_PATTERN, source).orElse("");
            String className = matchFirstGroup(CLASS_PATTERN, source).orElse(null);

            if (className == null) {
                return Optional.empty();
            }

            String qualifiedClassName = isBlank(packageName) ? className : packageName + "." + className;
            Class<?> clazz = Class.forName(qualifiedClassName);
            AlgorithmRunner runner = buildRunner(clazz);

            if (runner == null) {
                return Optional.empty();
            }

            Path relativePath = root.relativize(sourcePath.toAbsolutePath().normalize());
            return Optional.of(new AlgorithmEntry(runner.displayName(), relativePath.toString(), runner));
        } catch (IOException | ClassNotFoundException ignored) {
            return Optional.empty();
        }
    }

    private static AlgorithmRunner buildRunner(Class<?> clazz) {
        if (SortAlgorithm.class.isAssignableFrom(clazz)
                && !clazz.isInterface()
                && !Modifier.isAbstract(clazz.getModifiers())) {
            return new InterfaceRunner(clazz);
        }

        Method statsMethod = findSortMethod(clazz, true);
        if (statsMethod != null) {
            return new ReflectiveRunner(clazz, statsMethod, true);
        }

        Method basicMethod = findSortMethod(clazz, false);
        if (basicMethod != null) {
            return new ReflectiveRunner(clazz, basicMethod, false);
        }

        return null;
    }

    private static Method findSortMethod(Class<?> clazz, boolean withStats) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (!method.getName().equals("sort")) {
                continue;
            }

            Class<?>[] parameterTypes = method.getParameterTypes();
            if (withStats
                    && parameterTypes.length == 2
                    && parameterTypes[0] == int[].class
                    && parameterTypes[1] == SortStats.class) {
                method.setAccessible(true);
                return method;
            }

            if (!withStats && parameterTypes.length == 1 && parameterTypes[0] == int[].class) {
                method.setAccessible(true);
                return method;
            }
        }

        return null;
    }

    private static Optional<String> matchFirstGroup(Pattern pattern, String source) {
        Matcher matcher = pattern.matcher(source);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static int[] generateRandomArray(int size, int maxValue, long seed) {
        Random random = new Random(seed);
        int[] values = new int[size];

        for (int index = 0; index < size; index++) {
            values[index] = maxValue == 0 ? 0 : random.nextInt(maxValue + 1);
        }

        return values;
    }

    private static boolean isSorted(int[] array) {
        for (int index = 1; index < array.length; index++) {
            if (array[index - 1] > array[index]) {
                return false;
            }
        }
        return true;
    }

    private static String previewArray(int[] array) {
        StringBuilder builder = new StringBuilder("[");
        int limit = Math.min(array.length, PREVIEW_LENGTH);

        for (int index = 0; index < limit; index++) {
            if (index > 0) {
                builder.append(", ");
            }
            builder.append(array[index]);
        }

        if (array.length > PREVIEW_LENGTH) {
            builder.append(", ...");
        }

        builder.append("]");
        return builder.toString();
    }

    private static int readIntInRange(Scanner scanner, String prompt, int minimum, int maximum) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            try {
                int value = Integer.parseInt(input);
                if (value < minimum || value > maximum) {
                    System.out.printf("Enter a number between %d and %d.%n", minimum, maximum);
                    continue;
                }
                return value;
            } catch (NumberFormatException exception) {
                System.out.println("Enter a valid whole number.");
            }
        }
    }

    private static long readLong(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            try {
                return Long.parseLong(input);
            } catch (NumberFormatException exception) {
                System.out.println("Enter a valid whole number.");
            }
        }
    }

    private interface AlgorithmRunner {
        String displayName();

        void run(int[] array, SortStats stats) throws ReflectiveOperationException;
    }

    private static final class InterfaceRunner implements AlgorithmRunner {
        private final Class<?> clazz;

        private InterfaceRunner(Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        public String displayName() {
            try {
                SortAlgorithm algorithm = (SortAlgorithm) clazz.getDeclaredConstructor().newInstance();
                String name = algorithm.name();
                return isBlank(name) ? clazz.getSimpleName() : name;
            } catch (ReflectiveOperationException exception) {
                return clazz.getSimpleName();
            }
        }

        @Override
        public void run(int[] array, SortStats stats) throws ReflectiveOperationException {
            SortAlgorithm algorithm = (SortAlgorithm) clazz.getDeclaredConstructor().newInstance();
            algorithm.sort(array, stats);
        }
    }

    private static final class ReflectiveRunner implements AlgorithmRunner {
        private final Class<?> clazz;
        private final Method method;
        private final boolean acceptsStats;

        private ReflectiveRunner(Class<?> clazz, Method method, boolean acceptsStats) {
            this.clazz = clazz;
            this.method = method;
            this.acceptsStats = acceptsStats;
        }

        @Override
        public String displayName() {
            return clazz.getSimpleName() + (acceptsStats ? "" : " (time only)");
        }

        @Override
        public void run(int[] array, SortStats stats) throws ReflectiveOperationException {
            Object target = Modifier.isStatic(method.getModifiers())
                    ? null
                    : clazz.getDeclaredConstructor().newInstance();

            try {
                if (acceptsStats) {
                    method.invoke(target, array, stats);
                } else {
                    method.invoke(target, (Object) array);
                }
            } catch (InvocationTargetException exception) {
                Throwable cause = exception.getCause();
                if (cause instanceof ReflectiveOperationException) {
                    throw (ReflectiveOperationException) cause;
                }
                throw exception;
            }
        }
    }

    private static final class AlgorithmEntry {
        private final String displayName;
        private final String sourcePath;
        private final AlgorithmRunner runner;

        private AlgorithmEntry(String displayName, String sourcePath, AlgorithmRunner runner) {
            this.displayName = displayName;
            this.sourcePath = sourcePath;
            this.runner = runner;
        }
    }
}
