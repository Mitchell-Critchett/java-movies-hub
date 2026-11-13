package ru.practicum.moviehub.store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import ru.practicum.moviehub.model.Movie;

import java.util.HashMap;
import java.util.Optional;
import java.util.Random;

public class MoviesStore {

    private final Random random = new Random();

    private HashMap<Integer, Movie> moviesStore = new HashMap<>();

    private Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public MoviesStore() {
    }

    public String getAllMovies() {
        JsonArray jsonArray = new JsonArray();

        moviesStore.values()
                .forEach(movie -> jsonArray.add(gson.toJsonTree(movie)));

        return gson.toJson(jsonArray);
    }

    public String saveMovie(String title, int year) {
        int newId = random.nextInt(1, 100000);

        Movie newMovie = new Movie(newId, title, year);

        moviesStore.put(newId, newMovie);

        return gson.toJson(newMovie);
    }

    public Optional<String> getById(int id) {
        if (!moviesStore.containsKey(id)) {
            return Optional.empty();
        }
        return Optional.of(
                gson.toJson(moviesStore.get(id)));
    }

    public boolean deleteMovieById(int id) {
        if (!moviesStore.containsKey(id)) {
            return false;
        }

        Movie movieToDelete = moviesStore.remove(id);
        return true;
    }

    public String searchByYear(int year) {
        JsonArray jsonArray = new JsonArray();

        moviesStore.values().stream()
                .filter(movie -> movie.getYear() == year)
                .map(movie -> gson.toJsonTree(movie))
                .forEach(jsonArray::add);

        return gson.toJson(jsonArray);
    }
}