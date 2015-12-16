package popularmovie.project.garzoli.it.popularmovie.entity;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

/**
 * @author Francesco Garzoli
 * Date:   09-12-2015
 */
public class Movie implements Parcelable {

    private long id;
    private String title;
    private String overview;
    private String posterPath;
    private String backdropPath;
    private String director;
    private int duration;
    private String genres;
    private float rating;
    private Date releaseDate;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public String getPosterPath() {
        return posterPath;
    }

    public void setPosterPath(String posterPath) {
        this.posterPath = posterPath;
    }

    public String getBackdropPath() {
        return backdropPath;
    }

    public void setBackdropPath(String backdropPath) {
        this.backdropPath = backdropPath;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public String getGenres() {
        return genres;
    }

    public void setGenres(String genres) {
        this.genres = genres;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(title);
        dest.writeString(overview);
        dest.writeString(posterPath);
        dest.writeString(backdropPath);
        dest.writeFloat(rating);
        dest.writeLong(releaseDate.getTime());
    }

    public static final Creator<Movie> CREATOR = new Creator<Movie>() {
        @Override
        public Movie createFromParcel(Parcel source) {
            Movie movie = new Movie();
            movie.setId(source.readLong());
            movie.setTitle(source.readString());
            movie.setOverview(source.readString());
            movie.setPosterPath(source.readString());
            movie.setBackdropPath(source.readString());
            movie.setRating(source.readFloat());
            Date releaseDate = new Date();
            releaseDate.setTime(source.readLong());
            movie.setReleaseDate(releaseDate);
            return movie;
        }

        @Override
        public Movie[] newArray(int size) {
            return new Movie[size];
        }
    };
}


