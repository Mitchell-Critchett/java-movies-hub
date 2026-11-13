package ru.practicum.moviehub.model;

import java.util.Objects;

public class Movie {

    private final int id;

    private final String title;

    private final int year;

    public Movie(int id, String title, int year) {
        this.id = id;
        this.title = title;
        this.year = year;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Movie movie = (Movie) o;
        return id == movie.id && year == movie.year && Objects.equals(title, movie.title);
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + Objects.hashCode(title);
        result = 31 * result + year;
        return result;
    }

    @Override
    public String toString() {
        return "id фильма " + id + " " + "название фильма" + title + ", год выпуска: " + year;
    }

    public int getYear() {
        return year;
    }
}