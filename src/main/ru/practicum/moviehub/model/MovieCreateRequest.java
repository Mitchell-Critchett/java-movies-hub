package ru.practicum.moviehub.model;

public class MovieCreateRequest {
    public String title;
    public int year;

    public MovieCreateRequest(String title, int year) {
        this.title = title;
        this.year = year;
    }
}
