package popularmovie.project.garzoli.it.popularmovie;

/**
 * Created by Francesco Garzoli on 15/12/2015.
 */
public enum SortMovieCriteria {

        POPULARITY("popularity.desc"), RATING("vote_average.desc");
    SortMovieCriteria(String name) {
        this.name = name;
    }

        public final String name;

        public String toString() {
            return this.name;
        }
    }
