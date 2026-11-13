package ru.practicum.moviehub.api;

import java.util.ArrayList;

public class ErrorResponse {
    private String error;
    private ArrayList<String> details;

    public ErrorResponse(String error) {
        this.error = error;
    }

    public ErrorResponse(String error, ArrayList<String> details) {
        this.error = error;
        this.details = details;
    }
}