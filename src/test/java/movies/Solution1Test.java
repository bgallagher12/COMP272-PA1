package movies;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.ArrayList;


class Solution1Test extends MovieCatalogueTest {

    @Override
    protected MovieCatalogue createCatalogue() {
        return new Solution1();
    }

      @Test
    void shiftAndSearchCountersAreCorrect()
    {
        Solution1 solution = new Solution1();

        for(int i = 0; i < 1024; i++)
        {
            solution.insertMovie(new Movie(i, "Movie", 2000 + i, List.of()));   
        }
        assertEquals(0, solution.getShifts());

        solution.insertMovie(new Movie(5000, "Movie", 1990, List.of()));         
        assertEquals(1024, solution.getShifts());

        solution.resetCounters();
        solution.moviesInYears(2500, 2600);
        assertTrue(solution.getSearchSteps() <= 11);                           
    }

    @Test
    void loadAllSortsOnceWithoutShifting()
    {
        List<Movie> movies = new ArrayList<>();

        for(int i = 0; i < 3000; i++)
        {
            movies.add(new Movie(i, "Movie", 2100 - i % 120, List.of()));
        }

        Solution1 solution = new Solution1();
        solution.loadAll(movies);

        assertEquals(3000, solution.size());
        assertEquals(0, solution.getShifts());
        assertEquals(3000, solution.moviesInYears(1900, 2100).size());
        assertEquals(1981, solution.moviesInYears(1900, 2100).get(0).year());
    }

    @Test
    void topRatedCountsHeapOperations()
    {
        Solution1 solution = new Solution1();

        for(int i = 1; i <= 100; i++)
        {
            solution.insertMovie(new Movie(i, "Movie", 2000, List.of()));
            solution.addRating(i, 4.0);
        }

        solution.topRated(2000, 2000, 5, 1);
        assertEquals(100, solution.getHeapOperations());
    }
}