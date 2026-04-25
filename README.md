# sorting_algorithms
My attempt to create a bunch of sorting algorithms from scratch; starting simple with well-known methods, and eventually creating my own.

## Build and run

From the project root:

```powershell
.\build.bat
java Driver
```

The build script targets Java 8 because this machine currently runs `java` from Java 8 while `javac` is from JDK 25. If both commands use the same modern JDK, this also works:

```powershell
javac Driver.java algorithms\BubbleSort.java
java Driver
```
