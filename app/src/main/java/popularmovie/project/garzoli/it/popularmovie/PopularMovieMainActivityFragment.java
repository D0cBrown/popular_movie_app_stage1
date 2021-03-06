package popularmovie.project.garzoli.it.popularmovie;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import popularmovie.project.garzoli.it.popularmovie.entity.Movie;
import popularmovie.project.garzoli.it.popularmovie.loader.MoviesAdapter;

/**
 * @author Francesco Garzoli
 * Date:   09-12-2015
 */
public class PopularMovieMainActivityFragment extends Fragment {

    private final String LOG_TAG = PopularMovieMainActivityFragment.class.getSimpleName();
    private static final String STATE_MOVIES = "state_movies";
    private static final String STATE_SORT_CRITERIA = "state_sort_criteria";
    private static final String STATE_START_PAGE = "state_start_page";

    private MoviesAdapter mMoviesAdapter;
    private ArrayList<Movie> mMovieList;
    private ProgressBar mProgressBar;

    private int mTotalPageNumber = 1000;
    private SortMovieCriteria mSortCriteria = SortMovieCriteria.POPULARITY;
    private int mStartPage = 0;


    public void setSortCriteria(SortMovieCriteria criteria) {
        if (mSortCriteria != criteria) {
            mSortCriteria = criteria;
            updateMovies(1);
        }
    }

    public PopularMovieMainActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(LOG_TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_MOVIES)) {
            mMovieList = savedInstanceState.getParcelableArrayList(STATE_MOVIES);
        } else {
            mMovieList = new ArrayList<>();
        }
        Log.v(LOG_TAG, "onCreate > mMovieList size=" + (mMovieList != null ? mMovieList.size() : 0));
        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_SORT_CRITERIA)) {
            mSortCriteria = SortMovieCriteria.valueOf(savedInstanceState.getString(STATE_SORT_CRITERIA));
        }
        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_START_PAGE)) {
            mStartPage = savedInstanceState.getInt(STATE_START_PAGE);
        }
        Log.v(LOG_TAG, "onCreate > mStartPage=" + mStartPage + " mSortCriteria=" + mSortCriteria.toString());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_popular_movie_main_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();

        if (id == R.id.action_sort_popularity) {
            setSortCriteria(SortMovieCriteria.POPULARITY);
            return true;
        }
        if (id == R.id.action_sort_rating) {
            setSortCriteria(SortMovieCriteria.RATING);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_popular_movie_main, container, false);
        mMoviesAdapter = new MoviesAdapter(getActivity(), mMovieList);
        Log.v(LOG_TAG, "onCreateView() mMovieList size: " + mMovieList.size() + " mMoviesAdapter size: " + mMoviesAdapter.getCount() + " mStartPage=" + mStartPage);
        mProgressBar = (ProgressBar)rootView.findViewById(R.id.progressBar);
        mProgressBar.setVisibility(View.GONE);
        GridView gridView = (GridView) rootView.findViewById(R.id.movies_gridview);
        gridView.setAdapter(mMoviesAdapter);
        Log.d(LOG_TAG, "gridView.getNumColumns() = " + gridView.getNumColumns());
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Movie movie = mMoviesAdapter.getItem(position);
                Intent intent = new Intent(getActivity(), PopularMovieDetailActivity.class);
                intent.putExtra(BundleKeys.MOVIE, movie);
                getActivity().startActivity(intent);
            }
        });
        int visibleThreshold = 5;
        gridView.setOnScrollListener(new EndlessScrollListener(visibleThreshold, mStartPage) {
            @Override
            public boolean onLoadMore(int page, int totalItemsCount) {
                Log.v(LOG_TAG, "EndlessScrollListener.onLoadMore(" + page + ", " + totalItemsCount + ")");
                mStartPage = page - 1;
                loadMoreMoviesFromApi(page);
                return true;
            }
        });
        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(STATE_MOVIES, mMovieList);
        outState.putString(STATE_SORT_CRITERIA, mSortCriteria.name());
        outState.putInt(STATE_START_PAGE, mStartPage);
        Log.v(LOG_TAG, "onSaveInstanceState() > mStartPage: " + mStartPage);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        Log.v(LOG_TAG, "onViewStateRestored");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(LOG_TAG, "onResume");
    }

    private void loadMoreMoviesFromApi(int offset) {
        Log.v(LOG_TAG, "loadMoreMoviesFromApi(" + offset + ")");
        updateMovies(offset);
    }

    private void updateMovies(int page) {
        Log.v(LOG_TAG, "updateMovies(" + page + ") > fetch more mTotalPageNumber: " + mTotalPageNumber);
        if (page <= 1 && !mMovieList.isEmpty()) {
            Log.v(LOG_TAG, "clear mMovieList, mMoviesAdapter size: " + mMoviesAdapter.getCount());
            mStartPage = 0;
            mMovieList.clear();
            mMoviesAdapter.notifyDataSetChanged();
        }
        if (mTotalPageNumber == 0 || page <= mTotalPageNumber) {
            FetchMoviesTask task = new FetchMoviesTask();
            task.execute(mSortCriteria.toString(), Integer.toString(page));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.v(LOG_TAG, "onStart()");
        Log.v(LOG_TAG, "onStart() > mMovieList size: " + mMovieList.size() + " mStartPage=" + mStartPage);
        if (mMovieList.isEmpty()) {
            updateMovies(1);
        }
    }


    public class FetchMoviesTask extends AsyncTask<String, Void, ArrayList<Movie>> {
        private final String LOG_TAG = FetchMoviesTask.class.getSimpleName();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressBar.setVisibility(View.VISIBLE);
        }

        /**
         * Take the String representing the complete movies in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         * <p>
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         * </p>
         */
        private ArrayList<Movie> getMoviesPosterDataFromJson(String moviesJsonStr)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String MDB_RESULTS = "results";
//            final String MDB_PAGE = "page";
            final String MDB_TOTAL_PAGES = "total_pages";
            final String MDB_ID = "id";
            final String MDB_TITLE = "original_title";
            final String MDB_DESCRIPTION = "overview";
            final String MDB_POSTER_PATH = "poster_path";
            final String MDB_BACKDROP_PATH = "backdrop_path";
            final String MDB_RELEASE_DATE = "release_date";
            final String MDB_RATING = "vote_average";
            final String MDB_POPULARITY = "popularity";

            JSONObject moviesJson = new JSONObject(moviesJsonStr);
            int totalPages = moviesJson.getInt(MDB_TOTAL_PAGES);
            mTotalPageNumber = totalPages < mTotalPageNumber ? totalPages : mTotalPageNumber;
            JSONArray moviesArray = moviesJson.getJSONArray(MDB_RESULTS);
            final int moviesArraySize = moviesArray.length();
            final ArrayList<Movie> movies = new ArrayList<>(moviesArraySize);
            for (int i = 0; i < moviesArraySize; i++) {

                // Get the JSON object representing the movie
                JSONObject movieJson = moviesArray.getJSONObject(i);
                Movie movie = new Movie();
                movie.setId(movieJson.getLong(MDB_ID));
                movie.setTitle(movieJson.getString(MDB_TITLE));
                movie.setOverview(movieJson.getString(MDB_DESCRIPTION));
                movie.setPosterPath(movieJson.getString(MDB_POSTER_PATH));
                movie.setBackdropPath(movieJson.getString(MDB_BACKDROP_PATH));
                movie.setRating((float) movieJson.getDouble(MDB_RATING));
                Date releaseDate;
                String date = movieJson.getString(MDB_RELEASE_DATE);
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

                try {
                    releaseDate = formatter.parse(date);
                } catch (ParseException e) {
                    Log.e(LOG_TAG, "Parsing failed: " + Log.getStackTraceString(e));
                    releaseDate = new Date();
                }
                movie.setReleaseDate(releaseDate);

                if (BuildConfig.DEBUG) {
                    Log.v(LOG_TAG, "MDB_POPULARITY=" + movieJson.getString(MDB_POPULARITY) + "\t MDB_RATING= " + movie.getRating());
                }

                movies.add(movie);
            }

            return movies;
        }

        @Override
        protected ArrayList<Movie> doInBackground(String... params) {

            //If there's no sort definition, there's nothing to loop up. Verify size of params.
            if (params.length < 2) {
                return null;
            }
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String moviesJsonStr = null;

            try {
                // Construct the URL for the TheMovieDB API query
                // http://api.themoviedb.org/3/discover/movie?
                final String THEMOVIEDB_BASE_URL = "http://api.themoviedb.org/3/discover/movie?";
                final String SORT_PARAM = "sort_by";
                final String PAGE_PARAM = "page";
                final String MINVOTECOUNT_PARAM = "vote_count.gte";
                final String APPID_PARAM = "api_key";

                Uri builtUri = Uri.parse(THEMOVIEDB_BASE_URL).buildUpon()
                        .appendQueryParameter(SORT_PARAM, params[0]) //popularity.desc
                        .appendQueryParameter(PAGE_PARAM, params[1]) //1
                        .appendQueryParameter(MINVOTECOUNT_PARAM, "300")
                        .appendQueryParameter(APPID_PARAM, BuildConfig.THE_MOVIE_DB_API_KEY)
                        .build();

                URL url = new URL(builtUri.toString());
                Log.v(LOG_TAG, url.toString());

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuilder buffer = new StringBuilder();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line);
                    buffer.append("\n");

                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                moviesJsonStr = buffer.toString();

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the movie data, there's no point in attemping to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getMoviesPosterDataFromJson(moviesJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(ArrayList<Movie> movies) {
            if (movies != null && !movies.isEmpty()) {
                mMovieList.addAll(movies);
                mMoviesAdapter.notifyDataSetChanged();
                Log.v(LOG_TAG, "onPostExecute() mMovieList size: " + mMovieList.size() + " mMoviesAdapter size: " + mMoviesAdapter.getCount());
            }
            mProgressBar.setVisibility(View.GONE);
        }
    }
}
