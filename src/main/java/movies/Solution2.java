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
 * Uses a HashMap to store movies by their ID and a TreeMap
 * to store movies in order by release year and ID.
 *
 * The HashMap provides fast movie lookups, while the TreeMap
 * supports efficient searches across a range of years.
 * A PriorityQueue is used to find the top k rated movies.
 *
 * Data structures are from the Java Collections Framework
 * (java.util.HashMap, java.util.TreeMap, java.util.PriorityQueue).
 * The implementation was written by our team.
 *
 * Expected time complexities (n = total movies,
 * m = movies in the requested year range):
 *
 * insertMovie:   O(log n) average/expected
 * getById:       O(1) average
 * addRating:     O(1) average
 * ratingCount:   O(1) average
 * averageRating: O(1) average
 * moviesInYears: O(log n + m)
 * topRated:      O(log n + m log k + k^2)
 * size:          O(1)
 *
 * HashMap operations have O(n) worst-case bounds.
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