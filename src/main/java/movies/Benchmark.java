package movies;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Locale;
import java.io.BufferedReader;
import java.util.HashMap;

public class Benchmark {

    private static final Path MOVIES =
        Path.of("data/ml-32m/movies.csv");

    private static final Path RATINGS =
        Path.of("data/ml-32m/ratings.csv");

    private static final long SEED = 42;
    private static final int WARMUP_RUNS = 3;
    private static final int MEASURED_RUNS = 5;
    private static volatile long lookupSink;

    public static void main(String[] args) throws IOException {

        Locale.setDefault(Locale.US);

        Files.createDirectories(Path.of("results"));

        System.out.println("Loading movies...");

        List<Movie> movies = DatasetReader.readMovies(MOVIES);

        System.out.println("Movies loaded: " + movies.size());
        System.out.println("Skipped lines: "
            + DatasetReader.lastSkippedLines());
        System.out.println("Missing years: "
            + DatasetReader.lastMissingYear());

        System.out.println("Maximum heap: "
            + Runtime.getRuntime().maxMemory()
            / (1024 * 1024) + " MB");

        System.out.println("Benchmark initialized.");
        benchmarkBulkLoad(movies);
        benchmarkRatings(movies);
        benchmarkLookups(movies);
        benchmarkRanges(movies);
        benchmarkTopRated(movies);
        benchmarkMixed(movies);
        benchmarkMemory(movies);
    }

    private static void benchmarkBulkLoad(List<Movie> movies)
        throws IOException {

        int[] sizes = {
            50, 100, 1000, 10000, 50000, movies.size()
        };

        try (PrintWriter writer = new PrintWriter(
                Files.newBufferedWriter(
                    Path.of("results/bulk_load.csv")))) {

            writer.println(
                "solution,size,trial,time_ms,search_steps,"
                + "shifts,tree_comparisons"
            );

            for (int size : sizes) {

                List<Movie> subset = movies.subList(0, size);

                for (int solution = 1; solution <= 2; solution++) {

                    for (int trial = -WARMUP_RUNS;
                            trial < MEASURED_RUNS; trial++) {

                        MovieCatalogue catalogue;

                        if (solution == 1) {
                            catalogue = new Solution1();
                        } else {
                            catalogue = new Solution2();
                        }

                        long start = System.nanoTime();

                        for (Movie movie : subset) {
                            catalogue.insertMovie(movie);
                        }

                        long elapsed = System.nanoTime() - start;

                        long searchSteps = 0;
                        long shifts = 0;
                        long treeComparisons = 0;

                        if (catalogue instanceof Solution1 s1) {
                            searchSteps = s1.getSearchSteps();
                            shifts = s1.getShifts();
                        } else if (catalogue instanceof Solution2 s2) {
                            treeComparisons = s2.getTreeComparisons();
                        }

                        if (trial >= 0) {
                            writer.printf(
                                "Solution%d,%d,%d,%.6f,%d,%d,%d%n",
                                solution,
                                size,
                                trial + 1,
                                elapsed / 1_000_000.0,
                                searchSteps,
                                shifts,
                                treeComparisons
                            );
                        }
                    }

                    writer.flush();

                    System.out.println(
                        "Bulk load completed: Solution"
                        + solution + ", size = " + size
                    );
                }
            }
        }
    }
    private static void benchmarkRatings(List<Movie> movies)
        throws IOException {

        long[] sizes = {
            100_000,
            1_000_000,
            10_000_000,
            Long.MAX_VALUE
        };

        try (PrintWriter writer = new PrintWriter(
                Files.newBufferedWriter(
                    Path.of("results/rating_throughput.csv")))) {

            writer.println(
                "solution,requested_size,ratings_added,trial,"
                + "time_ms,ratings_per_second"
            );

            for (long size : sizes) {

                for (int solution = 1; solution <= 2; solution++) {

                    for (int trial = -WARMUP_RUNS;
                            trial < MEASURED_RUNS; trial++) {

                        MovieCatalogue catalogue;

                        if (solution == 1) {
                            catalogue = new Solution1();
                        } else {
                            catalogue = new Solution2();
                        }

                        for (Movie movie : movies) {
                            catalogue.insertMovie(movie);
                        }

                        long added = 0;
                        long processed = 0;
                        long elapsed = 0;

                        try (BufferedReader reader =
                                Files.newBufferedReader(RATINGS)) {

                            reader.readLine();

                            String line;

                            int[] ids = new int[10000];
                            double[] values = new double[10000];

                            while (processed < size) {

                                int count = 0;

                                while (count < ids.length
                                        && processed < size
                                        && (line = reader.readLine())
                                            != null) {

                                    String[] parts = line.split(",");

                                    ids[count] =
                                        Integer.parseInt(parts[1]);

                                    values[count] =
                                        Double.parseDouble(parts[2]);

                                    count++;
                                    processed++;
                                }

                                if (count == 0) {
                                    break;
                                }

                                long start = System.nanoTime();

                                for (int i = 0; i < count; i++) {
                                    if (catalogue.addRating(
                                            ids[i], values[i])) {
                                        added++;
                                    }
                                }

                                elapsed += System.nanoTime() - start;
                            }
                        }

                        double seconds = elapsed / 1_000_000_000.0;
                        double throughput = added / seconds;

                        if (trial >= 0) {
                            writer.printf(
                                "Solution%d,%s,%d,%d,%.6f,%.2f%n",
                                solution,
                                size == Long.MAX_VALUE
                                    ? "all" : Long.toString(size),
                                added,
                                trial + 1,
                                elapsed / 1_000_000.0,
                                throughput
                            );
                        }
                    }

                    writer.flush();

                    System.out.println(
                        "Rating benchmark completed: Solution"
                        + solution + ", size = "
                        + (size == Long.MAX_VALUE ? "all" : size)
                    );
                }
            }
        }
    }

    private static void benchmarkLookups(List<Movie> movies)
        throws IOException {

        int[] sizes = {
            50, 100, 1000, 10000, 50000, movies.size()
        };

        int operations = 100_000;

        try (PrintWriter writer = new PrintWriter(
                Files.newBufferedWriter(
                    Path.of("results/point_lookup.csv")))) {

            writer.println(
                "solution,size,workload,trial,operations,"
                + "time_ms,avg_latency_us,ops_per_second"
            );

            for (int size : sizes) {

                List<Movie> subset = movies.subList(0, size);

                int[] randomIds = new int[operations];
                int[] sequentialIds = new int[operations];

                Random random = new Random(SEED);

                for (int i = 0; i < operations; i++) {

                    randomIds[i] = subset.get(
                        random.nextInt(size)).id();

                    sequentialIds[i] =
                        subset.get(i % size).id();
                }

                for (int solution = 1; solution <= 2; solution++) {

                    MovieCatalogue catalogue;

                    if (solution == 1) {
                        catalogue = new Solution1();
                    } else {
                        catalogue = new Solution2();
                    }

                    for (Movie movie : subset) {
                        catalogue.insertMovie(movie);
                    }

                    for (String workload :
                            new String[]{"random", "sequential"}) {

                        int[] ids = workload.equals("random")
                            ? randomIds : sequentialIds;

                        for (int trial = -WARMUP_RUNS;
                                trial < MEASURED_RUNS; trial++) {

                            long checksum = 0;

                            long start = System.nanoTime();

                            for (int id : ids) {

                                Movie result = catalogue.getById(id);

                                if (result != null) {
                                    checksum += result.id();
                                }
                            }

                            long elapsed = System.nanoTime() - start;

                            lookupSink = checksum;

                            double latency =
                                elapsed / (operations * 1000.0);

                            double throughput =
                                operations /
                                (elapsed / 1_000_000_000.0);

                            if (trial >= 0) {

                                writer.printf(
                                    "Solution%d,%d,%s,%d,%d,"
                                    + "%.6f,%.6f,%.2f%n",
                                    solution,
                                    size,
                                    workload,
                                    trial + 1,
                                    operations,
                                    elapsed / 1_000_000.0,
                                    latency,
                                    throughput
                                );
                            }
                        }
                    }

                    writer.flush();

                    System.out.println(
                        "Lookup benchmark completed: Solution"
                        + solution + ", size = " + size
                    );
                }
            }
        }
    }
    
    private static void benchmarkRanges(List<Movie> movies)
        throws IOException {

        int[] sizes = {
            50, 100, 1000, 10000, 50000, movies.size()
        };

        int[] widths = {1, 10, 50};

        int operations = 500;

        try (PrintWriter writer = new PrintWriter(
                Files.newBufferedWriter(
                    Path.of("results/range_queries.csv")))) {

            writer.println(
                "solution,size,range_years,trial,operations,"
                + "time_ms,avg_latency_ms,ops_per_second,"
                + "avg_result_size,search_steps,tree_comparisons"
            );

            for (int size : sizes) {

                List<Movie> subset = movies.subList(0, size);

                int[] startYears = new int[operations];

                Random random = new Random(SEED);

                for (int i = 0; i < operations; i++) {
                    startYears[i] = 1950 + random.nextInt(51);
                }

                for (int solution = 1; solution <= 2; solution++) {

                    MovieCatalogue catalogue;

                    if (solution == 1) {
                        catalogue = new Solution1();
                    } else {
                        catalogue = new Solution2();
                    }

                    for (Movie movie : subset) {
                        catalogue.insertMovie(movie);
                    }

                    for (int width : widths) {

                        for (int trial = -WARMUP_RUNS;
                                trial < MEASURED_RUNS; trial++) {

                            if (catalogue instanceof Solution1 s1) {
                                s1.resetCounters();
                            } else if (catalogue instanceof Solution2 s2) {
                                s2.resetCounters();
                            }

                            long totalResults = 0;

                            long start = System.nanoTime();

                            for (int from : startYears) {

                                int to = from + width - 1;

                                List<Movie> result =
                                    catalogue.moviesInYears(from, to);

                                totalResults += result.size();
                            }

                            long elapsed = System.nanoTime() - start;

                            lookupSink = totalResults;

                            long searchSteps = 0;
                            long treeComparisons = 0;

                            if (catalogue instanceof Solution1 s1) {
                                searchSteps = s1.getSearchSteps();
                            } else if (catalogue instanceof Solution2 s2) {
                                treeComparisons = s2.getTreeComparisons();
                            }

                            double latency =
                                elapsed / (operations * 1_000_000.0);

                            double throughput =
                                operations /
                                (elapsed / 1_000_000_000.0);

                            double avgResultSize =
                                totalResults / (double) operations;

                            if (trial >= 0) {

                                writer.printf(
                                    "Solution%d,%d,%d,%d,%d,"
                                    + "%.6f,%.6f,%.2f,%.2f,%d,%d%n",
                                    solution,
                                    size,
                                    width,
                                    trial + 1,
                                    operations,
                                    elapsed / 1_000_000.0,
                                    latency,
                                    throughput,
                                    avgResultSize,
                                    searchSteps,
                                    treeComparisons
                                );
                            }
                        }
                    }

                    writer.flush();

                    System.out.println(
                        "Range benchmark completed: Solution"
                        + solution + ", size = " + size
                    );
                }
            }
        }
    }

    private static void benchmarkTopRated(List<Movie> movies)
        throws IOException {

        int[] widths = {1, 10, 50};

        int operations = 500;
        int k = 10;
        int minRatings = 50;

        int[] startYears = new int[operations];

        Random random = new Random(SEED);

        for (int i = 0; i < operations; i++) {
            startYears[i] = 1950 + random.nextInt(51);
        }

        try (PrintWriter writer = new PrintWriter(
                Files.newBufferedWriter(
                    Path.of("results/top_rated.csv")))) {

            writer.println(
                "solution,size,range_years,k,min_ratings,"
                + "trial,operations,time_ms,avg_latency_ms,"
                + "ops_per_second,avg_result_size,"
                + "search_steps,tree_comparisons,heap_operations"
            );

            for (int solution = 1; solution <= 2; solution++) {

                MovieCatalogue catalogue;

                if (solution == 1) {
                    catalogue = new Solution1();
                } else {
                    catalogue = new Solution2();
                }

                for (Movie movie : movies) {
                    catalogue.insertMovie(movie);
                }

                System.out.println(
                    "Loading ratings for Solution" + solution
                );

                long ratings = DatasetReader.replayRatings(
                    RATINGS, catalogue
                );

                System.out.println(
                    "Ratings replayed: " + ratings
                );

                for (int width : widths) {

                    for (int trial = -WARMUP_RUNS;
                            trial < MEASURED_RUNS; trial++) {

                        if (catalogue instanceof Solution1 s1) {
                            s1.resetCounters();
                        } else if (catalogue instanceof Solution2 s2) {
                            s2.resetCounters();
                        }

                        long totalResults = 0;

                        long start = System.nanoTime();

                        for (int from : startYears) {

                            int to = from + width - 1;

                            List<Movie> result =
                                catalogue.topRated(
                                    from, to, k, minRatings
                                );

                            totalResults += result.size();
                        }

                        long elapsed = System.nanoTime() - start;

                        lookupSink = totalResults;

                        long searchSteps = 0;
                        long treeComparisons = 0;
                        long heapOperations = 0;

                        if (catalogue instanceof Solution1 s1) {

                            searchSteps = s1.getSearchSteps();

                            heapOperations =
                                s1.getTotalHeapOperations();

                        } else if (catalogue instanceof Solution2 s2) {

                            treeComparisons =
                                s2.getTreeComparisons();

                            heapOperations =
                                s2.getHeapOperations();
                        }

                        double latency =
                            elapsed / (operations * 1_000_000.0);

                        double throughput =
                            operations /
                            (elapsed / 1_000_000_000.0);

                        double avgResultSize =
                            totalResults / (double) operations;

                        if (trial >= 0) {

                            writer.printf(
                                "Solution%d,%d,%d,%d,%d,%d,%d,"
                                + "%.6f,%.6f,%.2f,%.2f,%d,%d,%d%n",
                                solution,
                                movies.size(),
                                width,
                                k,
                                minRatings,
                                trial + 1,
                                operations,
                                elapsed / 1_000_000.0,
                                latency,
                                throughput,
                                avgResultSize,
                                searchSteps,
                                treeComparisons,
                                heapOperations
                            );
                        }
                    }

                    writer.flush();

                    System.out.println(
                        "Top-rated benchmark completed: Solution"
                        + solution + ", range = " + width
                    );
                }
            }
        }
    }
    private static void benchmarkMixed(List<Movie> movies)
        throws IOException {

        int[] sizes = {
            50, 100, 1000, 10000, 50000, movies.size()
        };

        int[] lookupPercentages = {90, 50};

        int operations = 10000;

        try (PrintWriter writer = new PrintWriter(
                Files.newBufferedWriter(
                    Path.of("results/mixed_workload.csv")))) {

            writer.println(
                "solution,size,lookup_percent,insert_percent,"
                + "trial,operations,lookups,inserts,time_ms,"
                + "avg_latency_us,ops_per_second,"
                + "search_steps,shifts,tree_comparisons"
            );

            for (int size : sizes) {

                List<Movie> subset = movies.subList(0, size);

                for (int lookupPercent : lookupPercentages) {

                    int[] lookupIds = new int[operations];
                    Movie[] insertMovies = new Movie[operations];
                    boolean[] isLookup = new boolean[operations];

                    Random random = new Random(SEED);

                    for (int i = 0; i < operations; i++) {

                        isLookup[i] =
                            i % 100 < lookupPercent;

                        lookupIds[i] = subset.get(
                            random.nextInt(size)).id();

                        insertMovies[i] = new Movie(
                            1_000_000 + i,
                            "Benchmark Movie " + i,
                            1950 + random.nextInt(75),
                            List.of()
                        );
                    }

                    for (int solution = 1;
                            solution <= 2; solution++) {

                        for (int trial = -WARMUP_RUNS;
                                trial < MEASURED_RUNS; trial++) {

                            MovieCatalogue catalogue;

                            if (solution == 1) {
                                catalogue = new Solution1();
                            } else {
                                catalogue = new Solution2();
                            }

                            for (Movie movie : subset) {
                                catalogue.insertMovie(movie);
                            }

                            if (catalogue instanceof Solution1 s1) {
                                s1.resetCounters();
                            } else if (catalogue instanceof Solution2 s2) {
                                s2.resetCounters();
                            }

                            long checksum = 0;
                            int lookups = 0;
                            int inserts = 0;

                            long start = System.nanoTime();

                            for (int i = 0; i < operations; i++) {

                                if (isLookup[i]) {

                                    Movie result =
                                        catalogue.getById(
                                            lookupIds[i]);

                                    if (result != null) {
                                        checksum += result.id();
                                    }

                                    lookups++;

                                } else {

                                    catalogue.insertMovie(
                                        insertMovies[i]);

                                    inserts++;
                                }
                            }

                            long elapsed =
                                System.nanoTime() - start;

                            lookupSink = checksum + catalogue.size();

                            long searchSteps = 0;
                            long shifts = 0;
                            long treeComparisons = 0;

                            if (catalogue instanceof Solution1 s1) {

                                searchSteps = s1.getSearchSteps();
                                shifts = s1.getShifts();

                            } else if (catalogue instanceof Solution2 s2) {

                                treeComparisons =
                                    s2.getTreeComparisons();
                            }

                            double latency =
                                elapsed / (operations * 1000.0);

                            double throughput =
                                operations /
                                (elapsed / 1_000_000_000.0);

                            if (trial >= 0) {

                                writer.printf(
                                    "Solution%d,%d,%d,%d,%d,"
                                    + "%d,%d,%d,%.6f,%.6f,"
                                    + "%.2f,%d,%d,%d%n",
                                    solution,
                                    size,
                                    lookupPercent,
                                    100 - lookupPercent,
                                    trial + 1,
                                    operations,
                                    lookups,
                                    inserts,
                                    elapsed / 1_000_000.0,
                                    latency,
                                    throughput,
                                    searchSteps,
                                    shifts,
                                    treeComparisons
                                );
                            }
                        }

                        writer.flush();

                        System.out.println(
                            "Mixed benchmark completed: Solution"
                            + solution
                            + ", size = " + size
                            + ", lookup percentage = "
                            + lookupPercent
                        );
                    }
                }
            }
        }
    }
    private static long usedMemory() {

        Runtime runtime = Runtime.getRuntime();

        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static void collectGarbage() {

        System.gc();

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void benchmarkMemory(List<Movie> movies)
        throws IOException {

        int[] sizes = {
            50, 100, 1000, 10000, 50000, movies.size()
        };

        try (PrintWriter writer = new PrintWriter(
                Files.newBufferedWriter(
                    Path.of("results/memory_usage.csv")))) {

            writer.println(
                "solution,size,trial,memory_bytes,"
                + "memory_mb,bytes_per_movie"
            );

            for (int size : sizes) {

                List<Movie> subset = movies.subList(0, size);

                for (int solution = 1; solution <= 2; solution++) {

                    for (int trial = -WARMUP_RUNS;
                            trial < MEASURED_RUNS; trial++) {

                        collectGarbage();

                        long before = usedMemory();

                        MovieCatalogue catalogue;

                        if (solution == 1) {
                            catalogue = new Solution1();
                        } else {
                            catalogue = new Solution2();
                        }

                        for (Movie movie : subset) {
                            catalogue.insertMovie(movie);
                        }

                        collectGarbage();

                        long after = usedMemory();

                        long memory = after - before;

                        double memoryMB =
                            memory / (1024.0 * 1024.0);

                        double bytesPerMovie =
                            memory / (double) size;

                        // Keep catalogue alive through measurement.
                        java.lang.ref.Reference.reachabilityFence(
                            catalogue
                        );

                        if (trial >= 0) {

                            writer.printf(
                                "Solution%d,%d,%d,%d,%.6f,%.2f%n",
                                solution,
                                size,
                                trial + 1,
                                memory,
                                memoryMB,
                                bytesPerMovie
                            );
                        }
                    }

                    writer.flush();

                    System.out.println(
                        "Memory benchmark completed: Solution"
                        + solution + ", size = " + size
                    );
                }
            }
        }
    }
}
