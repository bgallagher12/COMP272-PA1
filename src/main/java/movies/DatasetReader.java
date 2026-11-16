package movies;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the MovieLens dataset (movies.csv and ratings.csv).
 * Dataset: F. M. Harper and J. A. Konstan, "The MovieLens Datasets: History and Context",
 * ACM TiiS 5(4), 2015. https://grouplens.org/datasets/movielens/
 *
 * 
 *
 */

public final class DatasetReader {

    
    public record Rating(int movieId, double rating) 
    {

    }

    private static final Pattern YEAR = Pattern.compile("\\((\\d{4})\\)\\s*$");

    private static final String NO_GENRES = "(no genres listed)";

    private static int skippedLines;
    private static int moviesWithNoYear;

    private DatasetReader() 
    { 

    }   
 
    public static int lastSkippedLines() 
    {
        return skippedLines;
    }

    public static int lastMissingYear() 
    {
        return moviesWithNoYear;
    }

    public static List<Movie> readMovies(Path moviesCsv) throws IOException
    {
        List<Movie> movies = new ArrayList<>();
        skippedLines = 0;
        moviesWithNoYear = 0;
        try (BufferedReader br = Files.newBufferedReader(moviesCsv)) 
        {
            String line = br.readLine();               // header
            while ((line = br.readLine()) != null) 
                {
                    Movie m = readMovieLine(line);
                    
                    if(m == null)
                    {
                        skippedLines++;
                    }
                    else
                    {
                        movies.add(m);
                    }
                }
        }
        return movies;
    }

    private static Movie readMovieLine(String line) 
    {
        int id;
        String title;
        int year = 0;
        int first = line.indexOf(',');
        int last = line.lastIndexOf(',');

        if (first < 0 || last <= first) 
        {
            return null;
        }
        try 
        {
            id = Integer.parseInt(line.substring(0, first));
            title = line.substring(first + 1, last);

            if(title.length() >= 2 && title.startsWith("\"") && title.endsWith("\""))
            {
                title = title.substring(1, title.length() - 1).replace("\"\"", "\"");
            }

            Matcher m = YEAR.matcher(title);

            if(m.find())
            {
                year = Integer.parseInt(m.group(1));
                title = title.substring(0, m.start()).trim();
            }
            else
            {
                moviesWithNoYear++;
            }

            String g = line.substring(last + 1);
            List<String> genre = g.equals("(No Genres Were Listed)") ? List.of() : List.of(g.split("\\|"));

            return new Movie(id, title, year, genre);
        } 
        catch (NumberFormatException e) 
        {
            return null;
        }
    }
    /**
     * Reads at most maxLines ratings of ratings.csv, in file order.
     *
     * <p>Memory warning: a list of all 32 million ratings of ml-32m needs well over
     * 1 GB. Use maxLines (e.g. 5,000,000) to read part of the file, or
     * {@link #forEachRating} to replay the whole file without storing it.
     */
    public static List<Rating> readRatings(Path ratingsCsv, int maxLines) throws IOException 
    {
        List<Rating> ratings = new ArrayList<>();
        skippedLines = 0;
        
        try(BufferedReader br = Files.newBufferedReader(ratingsCsv))
        {
            String line = br.readLine();

            while(ratings.size() < maxLines && (line = br.readLine()) != null)
            {
                Rating r = readRatingLine(line);

                if(r == null)
                {
                    skippedLines++;
                }
                else
                {
                    ratings.add(r);
                }
            }
        }
        return ratings;
    }

    public static long replayRatings(Path ratingsCsv, MovieCatalogue cat) throws IOException
    {
        long added = 0;
        skippedLines = 0;

        try(BufferedReader br = Files.newBufferedReader(ratingsCsv))
        {
            String line = br.readLine();

            while((line = br.readLine()) != null)
            {
                Rating r = readRatingLine(line);

                if(r == null)
                {
                    skippedLines++;
                }
                else
                {
                    cat.addRating(r.movieId(), r.rating());
                    added++;
                }
            }
        }
        return added;
    }

    private static Rating readRatingLine(String line)
    {
        String[] p = line.split(",");

        if(p.length < 3)
        {
            return null;
        }
        try
        {
            double rating = Double.parseDouble(p[2]);

            if(rating < 0.5 || rating > 5.0)
            {
                return null;
            }
            return new Rating(Integer.parseInt(p[1]), rating);
        }
        catch(NumberFormatException e)
        {
            return null;
        }
    }

}

   