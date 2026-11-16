package movies;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;



public class Solution1 implements MovieCatalogue {

    static class MovieData
    {
        Movie movie;
        int ratingnumber;
        double ratingtotal;

        MovieData(Movie movie)
        {
            this.movie = movie;
        }

        long key()
        {
            return ((long) this.movie.year() << 32) | this.movie.id();
        }

        double average()
        {
            if(this.ratingnumber == 0)
            {
                return 0.0;
            }

            return this.ratingtotal / this.ratingnumber;
        }

    }

    public static final boolean COUNTERS = true;
    private long searchSteps;
    private long shifts;
    private long heapOperations;

    private final HashMap<Integer, MovieData> byId;
    private final ArrayList<MovieData> byYear;

    public Solution1() 
    {
        this.byId = new HashMap<>();
        this.byYear = new ArrayList<>();
    }

    @Override
    public void insertMovie(Movie movie) 
    {
        MovieData data = this.byId.get(movie.id());

        if (data == null) 
        {
            data = new MovieData(movie);
            this.byId.put(movie.id(), data);
            addToList(data);
        }
        else if(data.movie.year() == movie.year())
        {
            data.movie = movie;
        }
        else
        {
            removeFromList(data);
            data.movie = movie;
            addToList(data);
        }
    }

    @Override
    public Movie getById(int id) 
    {
        MovieData data = this.byId.get(id);

        if(data == null)
        {
            return null;
        }

        return data.movie;
    }

    @Override
    public boolean addRating(int movieId, double rating) 
    {
        MovieData data = this.byId.get(movieId);

        if(data == null)
        {
            return false;
        }

        data.ratingnumber++;
        data.ratingtotal += rating;

        return true;
    }

    @Override
    public int ratingCount(int movieId) 
    {
        MovieData data = this.byId.get(movieId);

        if(data == null)
        {
            return 0;
        }

        return data.ratingnumber;
    }

    @Override
    public double averageRating(int movieId)
    {
        MovieData data = this.byId.get(movieId);

        if(data == null)
        {
            return 0.0;
        }

        return data.average();
    }
        

    @Override
    public List<Movie> moviesInYears(int from, int to) 
    {
        List<Movie> result = new ArrayList<>();

        if(from > to)
        {
            return result;
        }

        long last = ((long) to << 32) | Integer.MAX_VALUE;

        for(int i = findFirst((long) from << 32); i < this.byYear.size() && this.byYear.get(i).key() <= last; i++)
        {
            result.add(this.byYear.get(i).movie);
        }

        return result;
    }

    @Override
    public List<Movie> topRated(int from, int to, int k, int minRatings) 
    {
        List<Movie> result = new ArrayList<>();

        if(k <= 0 || from > to)
        {
            return result;
        }

        PriorityQueue<MovieData> heap = new PriorityQueue<>((x, y) -> rank(x, y));

        long last = ((long) to << 32) | Integer.MAX_VALUE;

        for(int i = findFirst((long) from << 32); i < this.byYear.size() && this.byYear.get(i).key() <= last; i++)
        {
            MovieData data = this.byYear.get(i);

            if(data.ratingnumber > 0 && data.ratingnumber >= minRatings)
            {
                heap.add(data);

                if(heap.size() > k)
                {
                    heap.poll();
                }
                if(COUNTERS)
                {
                    this.heapOperations++;
                }
            }
        }
        while(!heap.isEmpty())
        {
            result.add(0, heap.poll().movie);
        }

        return result;
    }

    @Override
    public int size() 
    {
        return this.byId.size();
    }


    public void loadAll(List<Movie> movies)
    {
        for(Movie movie : movies)
        {
            MovieData data = this.byId.get(movie.id());

            if(data == null)
            {
                this.byId.put(movie.id(), new MovieData(movie));
            }
            else
            {
                data.movie = movie;
            }
        }

        this.byYear.clear();
        this.byYear.addAll(this.byId.values());
        this.byYear.sort((a, b) -> Long.compare(a.key(), b.key()));
    }

    private static int rank(MovieData x, MovieData y)
    {
        if(x.average() != y.average())
        {
            return Double.compare(x.average(), y.average());
        }

        return Integer.compare(y.movie.id(), x.movie.id());
    }

    private int findFirst(long key)
    {
        int lower = 0;
        int higher = this.byYear.size();

        while(lower < higher)
        {
            int middle = (lower + higher) / 2;

            if(this.byYear.get(middle).key() < key)
            {
                lower = middle + 1;
            }
            else
            {
                higher = middle;
            }
            if(COUNTERS)
            {
                this.searchSteps++;
            }
        }

        return lower;

    }

    private void addToList(MovieData data)
    {
        int position = findFirst(data.key());

        this.byYear.add(position, data);
        
        if(COUNTERS)
        {
            this.shifts += this.byYear.size() - position;
        }
    }

    private void removeFromList(MovieData data)
    {
        int position = findFirst(data.key());

        if(COUNTERS)
        {
            this.shifts += this.byYear.size() - position - 1;
        }

        this.byYear.remove(position);
    }

    public long getSearchSteps()
    {
        return this.searchSteps;
    }

    public long getShifts()
    {
        return this.shifts;
    }

    public long getHeapOperations()
    {
        return this.heapOperations;
    }

    public void resetCounters()
    {
        this.searchSteps = 0;
        this.shifts = 0;
        this.heapOperations = 0;
    }
}

    

