package movies;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Tests shared by both solutions. They are written once, against the MovieCatalogue
 * interface; Solution1Test and Solution2Test run them on each solution.
 *
 * <p>These tests are a STARTING POINT. You must add more (see the TODO list at the end).
 */
abstract class MovieCatalogueTest {

    /** Returns a new, empty catalogue of the solution being tested. */
    protected abstract MovieCatalogue createCatalogue();

    private MovieCatalogue cat;

    @BeforeEach
    void setUp() {
        cat = createCatalogue();   // a fresh, empty catalogue before every test
    }

    // Small helpers ------------------------------------------------------

    private static Movie movie(int id, String title, int year) {
        return new Movie(id, title, year, List.of());
    }

    /** The IDs of a list of movies, in order: makes assertions short and readable. */
    private static List<Integer> ids(List<Movie> movies) {
        List<Integer> result = new ArrayList<>();
        for (Movie m : movies) {
            result.add(m.id());
        }
        return result;
    }

    private void addRatings(int movieId, double... ratings) {
        for (double r : ratings) {
            cat.addRating(movieId, r);
        }
    }

    // ------------------------------------------------------------------
    // insertMovie, getById, size
    // ------------------------------------------------------------------

    @Test
    void newCatalogueIsEmpty() {
        assertEquals(0, cat.size());
        assertNull(cat.getById(1));
    }

    @Test
    void insertedMovieCanBeFoundById() {
        Movie toy = movie(1, "Toy Story", 1995);
        cat.insertMovie(toy);
        assertEquals(toy, cat.getById(1));
        assertEquals(1, cat.size());
    }

    @Test
    void missingIdReturnsNull() {
        cat.insertMovie(movie(1, "Toy Story", 1995));
        assertNull(cat.getById(2));
    }

    @Test
    void insertingAnExistingIdReplacesTheMovieAndKeepsItsRatings() {
        cat.insertMovie(movie(1, "Toy Story", 1995));
        addRatings(1, 4.0, 5.0);
        Movie corrected = movie(1, "Toy Story (corrected)", 1996);
        cat.insertMovie(corrected);
        assertEquals(corrected, cat.getById(1));
        assertEquals(1, cat.size());
        assertEquals(2, cat.ratingCount(1));
    }

    @Test
    void replacingAMovieMovesItToItsNewYear() {
        cat.insertMovie(movie(1, "Toy Story", 1995));
        cat.insertMovie(movie(1, "Toy Story", 1996));
        assertEquals(List.of(), cat.moviesInYears(1995, 1995));
        assertEquals(List.of(1), ids(cat.moviesInYears(1996, 1996)));
    }

    // ------------------------------------------------------------------
    // addRating, ratingCount, averageRating
    // ------------------------------------------------------------------

    @Test
    void ratingsAreCountedAndAveraged() {
        cat.insertMovie(movie(1, "Toy Story", 1995));
        addRatings(1, 4.0, 5.0, 3.5);
        assertEquals(3, cat.ratingCount(1));
        assertEquals(12.5 / 3, cat.averageRating(1), 1e-9);
    }

    @Test
    void movieWithoutRatingsHasCountAndAverageZero() {
        cat.insertMovie(movie(1, "Toy Story", 1995));
        assertEquals(0, cat.ratingCount(1));
        assertEquals(0.0, cat.averageRating(1), 1e-9);
    }

    @Test
    void ratingForAMissingMovieIsIgnored() {
        assertFalse(cat.addRating(99, 4.0));
        assertEquals(0, cat.ratingCount(99));
        assertEquals(0.0, cat.averageRating(99), 1e-9);
    }

    // ------------------------------------------------------------------
    // moviesInYears
    // ------------------------------------------------------------------

    @Test
    void moviesInYearsIncludesBothEndsAndIsSortedByYearThenId() {
        cat.insertMovie(movie(30, "C", 1999));
        cat.insertMovie(movie(10, "A", 1995));
        cat.insertMovie(movie(20, "B", 1995));   // same year as movie 10
        cat.insertMovie(movie(5, "D", 2000));
        cat.insertMovie(movie(40, "E", 2001));   // outside the range
        cat.insertMovie(movie(50, "F", 1994));   // outside the range
        assertEquals(List.of(10, 20, 30, 5), ids(cat.moviesInYears(1995, 2000)));
    }

    @Test
    void manyMoviesInTheSameYearAreAllReturned() {
        for (int id = 1; id <= 50; id++) {
            cat.insertMovie(movie(id, "Movie " + id, 2010));
        }
        assertEquals(50, cat.moviesInYears(2010, 2010).size());
    }

    @Test
    void emptyOrReversedRangeReturnsAnEmptyList() {
        cat.insertMovie(movie(1, "Toy Story", 1995));
        assertEquals(List.of(), cat.moviesInYears(2000, 2005));
        assertEquals(List.of(), cat.moviesInYears(2000, 1990));
    }

    @Test
    void movieWithUnknownYearIsNotInARangeOfRealYears() {
        cat.insertMovie(movie(1, "No year", 0));
        cat.insertMovie(movie(2, "Toy Story", 1995));
        assertEquals(List.of(2), ids(cat.moviesInYears(1900, 2100)));
    }

    // ------------------------------------------------------------------
    // topRated
    // ------------------------------------------------------------------

    @Test
    void topRatedIsSortedByAverageThenId() {
        cat.insertMovie(movie(1, "A", 2000));
        cat.insertMovie(movie(2, "B", 2001));
        cat.insertMovie(movie(3, "C", 2002));
        cat.insertMovie(movie(4, "D", 2003));
        addRatings(1, 3.0);
        addRatings(2, 5.0);
        addRatings(3, 4.0, 5.0);   // average 4.5
        addRatings(4, 3.0);        // same average as movie 1
        assertEquals(List.of(2, 3, 1, 4), ids(cat.topRated(2000, 2010, 10, 1)));
        assertEquals(List.of(2, 3), ids(cat.topRated(2000, 2010, 2, 1)));
    }

    @Test
    void topRatedIgnoresMoviesWithTooFewRatingsOrNone() {
        cat.insertMovie(movie(1, "A", 2000));
        cat.insertMovie(movie(2, "B", 2000));
        cat.insertMovie(movie(3, "C", 2000));
        addRatings(1, 5.0);              // only 1 rating
        addRatings(2, 3.0, 4.0, 3.0);    // 3 ratings
        // movie 3 has no rating
        assertEquals(List.of(2), ids(cat.topRated(2000, 2000, 10, 2)));
        assertEquals(List.of(1, 2), ids(cat.topRated(2000, 2000, 10, 0)));
    }

    @Test
    void topRatedOnlyConsidersTheYearRange() {
        cat.insertMovie(movie(1, "Old", 1950));
        cat.insertMovie(movie(2, "New", 2020));
        addRatings(1, 5.0);
        addRatings(2, 2.0);
        assertEquals(List.of(2), ids(cat.topRated(2000, 2030, 5, 1)));
    }

    @Test
    void topRatedWithKZeroReturnsAnEmptyList() {
        cat.insertMovie(movie(1, "A", 2000));
        addRatings(1, 4.0);
        assertEquals(List.of(), cat.topRated(1990, 2010, 0, 1));
    }

    // ------------------------------------------------------------------
    // Larger inputs
    // ------------------------------------------------------------------

    @Test
    void manyMoviesInsertedInIncreasingIdOrderAreAllFound() {
        // movies.csv is sorted by ID: the worst case for an unbalanced BST keyed by ID.
        int n = 20_000;
        for (int id = 1; id <= n; id++) {
            cat.insertMovie(movie(id, "Movie " + id, 1900 + id % 120));
        }
        assertEquals(n, cat.size());
        for (int id = 1; id <= n; id++) {
            assertEquals(id, cat.getById(id).id());
        }
        assertEquals(n, cat.moviesInYears(1900, 2019).size());
    }

    @Test
    void randomRangeQueriesGiveTheSameResultsAsASimpleReference() {
        // The reference is a plain HashMap plus sorting: slow, but obviously correct.
        Map<Integer, Movie> reference = new HashMap<>();
        Random random = new Random(42);   // fixed seed: the test is repeatable
        for (int i = 0; i < 5_000; i++) {
            Movie m = movie(random.nextInt(3_000), "M", 1900 + random.nextInt(121));
            cat.insertMovie(m);           // some IDs repeat: tests replacement too
            reference.put(m.id(), m);
        }
        assertEquals(reference.size(), cat.size());

        for (int q = 0; q < 300; q++) {
            int from = 1900 + random.nextInt(121);
            int to = from + random.nextInt(30);
            List<Movie> expected = new ArrayList<>();
            for (Movie m : reference.values()) {
                if (m.year() >= from && m.year() <= to) {
                    expected.add(m);
                }
            }
            expected.sort(Comparator.comparingInt(Movie::year).thenComparingInt(Movie::id));
            assertEquals(ids(expected), ids(cat.moviesInYears(from, to)), from + "-" + to);
        }
    }

    // ------------------------------------------------------------------
    // Tests from the dataset
    // ------------------------------------------------------------------

    @Test
    void everyMovieOfTheDatasetCanBeFound() throws IOException {
        List<Movie> movies = DatasetReader.readMovies(Path.of("data", "movies.csv"));
        for (Movie m : movies) {
            cat.insertMovie(m);
        }
        assertEquals(movies.size(), cat.size());
        for (Movie m : movies) {
            assertEquals(m, cat.getById(m.id()));
        }
        assertEquals(movies.size(), cat.moviesInYears(0, 3000).size());
    }

    @Test
    void updatingMovieYearPreservesRatingsAndChangesYearIndex() {
        Movie original = new Movie(100, "Test Movie", 2000, List.of());
        cat.insertMovie(original);

        cat.addRating(100, 4.0);
        cat.addRating(100, 5.0);

        Movie updated = new Movie(100, "Updated Movie", 2005, List.of());
        cat.insertMovie(updated);

        assertEquals(2, cat.ratingCount(100));
        assertEquals(4.5, cat.averageRating(100), 1e-9);
        assertTrue(cat.moviesInYears(2000, 2000).isEmpty());
        assertEquals(List.of(updated), cat.moviesInYears(2005, 2005));
    }

    @Test 
    void datasetMoviesAreReadCorrectly() throws IOException
    {
        for(Movie movie : DatasetReader.readMovies(Path.of("data", "movies.csv")))
        {
            cat.insertMovie(movie);
        }

        assertEquals(9742, cat.size());
        assertEquals(13, DatasetReader.lastMissingYear());
        assertEquals("Toy Story", cat.getById(1).title());
        assertEquals("Matrix, The", cat.getById(2571).title());
        assertEquals(new Movie(73881, "3 Idiots", 2009, List.of("Comedy", "Drama", "Romance")), cat.getById(73881));
        assertEquals("Like Stars on Earth (Taare Zameen Par)", cat.getById(57183).title());
        assertEquals(2007, cat.getById(57183).year());
        assertEquals(1931, cat.moviesInYears(2010, 2019).size());
    }

    @Test 
    void datasetRatingsAreReadCorrectly() throws IOException
    {
        for(Movie movie : DatasetReader.readMovies(Path.of("data", "movies.csv")))
        {
            cat.insertMovie(movie);
        }

        assertEquals(100836, DatasetReader.replayRatings(Path.of("data", "ratings.csv"), cat));
        assertEquals(215, cat.ratingCount(1));
        assertEquals(843.0 / 215, cat.averageRating(1), 1e-9);
        assertEquals(318, cat.topRated(1990, 1999, 1, 100).get(0).id());
    }

    @Test
    void randomTopRatedMatchesAReference()
    {
        Random random = new Random(272);
        int[] years = new int[500];
        int[] ratingnumber = new int[500];
        double[] ratingtotal = new double[500];

        for(int id = 0; id < 500; id++)
        {
            years[id] = 1900 + random.nextInt(30);
            cat.insertMovie(movie(id, "Movie", years[id]));
        }
        for(int i = 0; i < 5000; i++)
        {
            int id = random.nextInt(500);
            double rating = 0.5 * (1 + random.nextInt(10));
            cat.addRating(id, rating);
            ratingnumber[id]++;
            ratingtotal[id] += rating;
        }
        for(int j = 0; j < 200; j++)
        {
            int from = 1900 + random.nextInt(30);
            int to = from + random.nextInt(10);
            int minimumRatings = random.nextInt(15);
            List<Movie> expected = new ArrayList<>();

            for(int id = 0; id < 500; id++)
            {
                if(years[id] >= from && years[id] <= to && ratingnumber[id] >= minimumRatings)
                {
                    expected.add(cat.getById(id));
                }
            }

            expected.sort(Comparator.comparingDouble((Movie movie) -> - ratingtotal[movie.id()]/ratingnumber[movie.id()])
            .thenComparingInt(Movie::id));
            assertEquals(ids(expected.subList(0, Math.min(10, expected.size()))), ids(cat.topRated(from, to, 10, minimumRatings)));
        }
    }
}
