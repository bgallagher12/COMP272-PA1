package movies;

import java.util.List;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.PriorityQueue;

/**
 * Solution 2 for the movie catalogue.
 *
 * <p>TODO: Replace this comment with a short description of your solution:
 * <ul>
 *   <li>the data structure(s) used (e.g. "a Red-Black tree by ID, plus a sorted array list by year");</li>
 *   <li>where the code comes from: written by your team, adapted from course code,
 *       or a library class (e.g. java.util.TreeMap). Cite the source;</li>
 *   <li>the expected time complexity of each method below.</li>
 * </ul>
 * The full explanation of the implementation and of its expected performance
 * goes in your report.
 *
 * <p>You may rename this class to something more descriptive (e.g. TreeCatalogue).
 * If you do, also rename it in Solution2Test.
 */
public class Solution2 implements MovieCatalogue {

    private static class MovieData {
        Movie movie;
        int ratingCount;
        double ratingSum;

        MovieData(Movie movie) {
            this.movie = movie;
            this.ratingCount = 0;
            this.ratingSum = 0.0;
        }
    }

            private record YearKey(int year, int id) {}

            private final HashMap<Integer, MovieData> moviesById;

            private final TreeMap<YearKey, MovieData> moviesByYear;

    public Solution2() {
        moviesById = new HashMap<>();

        moviesByYear = new TreeMap<>(
            Comparator.comparingInt(YearKey::year)
                      .thenComparingInt(YearKey::id)
        );
    }


    @Override
    public void insertMovie(Movie movie) {
        MovieData existing = moviesById.get(movie.id());
        if (existing == null) {
            MovieData data = new MovieData(movie);
            moviesById.put(movie.id(), data);
            moviesByYear.put(new YearKey(movie.year(), movie.id()), data);
        }
        else {
            moviesByYear.remove(new YearKey(existing.movie.year(), movie.id()));
            existing.movie = movie;
            moviesByYear.put(new YearKey(movie.year(), movie.id()), existing); 
        }

    }

    @Override
    public Movie getById(int id) {
        MovieData data = moviesById.get(id);
        if (data == null) {
            return null;
        }
        else {
            return data.movie;
        }
    }

    @Override
    public boolean addRating(int movieId, double rating) {
        MovieData data = moviesById.get(movieId);
        if (data == null) {
            return false;
        }
        else {
            data.ratingCount++;
            data.ratingSum += rating;
            return true;
        }
    }

    @Override
    public int ratingCount(int movieId) {
        MovieData data = moviesById.get(movieId);
        if (data == null){
            return 0;
        }
        else {
            return data.ratingCount;
        }
    }

    @Override
    public double averageRating(int movieId) {
        MovieData data = moviesById.get(movieId);
        if (data == null || data.ratingCount == 0) {
            return 0.0;
        }
        else {
            return (data.ratingSum / data.ratingCount);
        }
    }

    @Override
    public List<Movie> moviesInYears(int from, int to) {
        List<Movie> results = new ArrayList<>();
        if (from > to){
            return results;
        }
        else {
            for (MovieData data : moviesByYear.subMap(
                new YearKey(from, Integer.MIN_VALUE), true,
                new YearKey(to, Integer.MAX_VALUE), true
            ).values()) {
                results.add(data.movie);
            }
        }
        return results;
    }

    @Override
    public List<Movie> topRated(int from, int to, int k, int minRatings) {
        List<Movie> results = new ArrayList<>();

        if (k <= 0 || from > to) {
            return results;
        }
        Comparator<MovieData> worstFirst = (a, b) -> {
            double avgA = a.ratingSum / a.ratingCount;
            double avgB = b.ratingSum / b.ratingCount;

            int comparison = Double.compare(avgA, avgB);

            if (comparison != 0) {
                return comparison;
            }

            return Integer.compare(b.movie.id(), a.movie.id());
        };

        PriorityQueue<MovieData> topMovies = new PriorityQueue<>(worstFirst);

        for (MovieData data : moviesByYear.subMap(
                new YearKey(from, Integer.MIN_VALUE), true,
                new YearKey(to, Integer.MAX_VALUE), true
            ).values()) {
                if (data.ratingCount == 0 || data.ratingCount < minRatings) {
                    continue;
                }
                if (topMovies.size() < k) {
                    topMovies.offer(data);
                }
                else {
                    if (worstFirst.compare(data, topMovies.peek()) > 0) {
                        topMovies.poll();
                        topMovies.offer(data);
                    }
                }       
            }   
            while (!topMovies.isEmpty()) {
                results.add(0, topMovies.poll().movie);
            }

            return results;
    }



    @Override
    public int size() {
        return moviesById.size();
    }

}