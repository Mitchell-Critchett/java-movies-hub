package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.MovieCreateRequest;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

public class MoviesHandler extends BaseHttpHandler { // Расширьте базовый класс BaseHttpHandler

    private final MoviesStore moviesStore;

    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();

        switch (method) {

            case "GET": {
                String query = ex.getRequestURI().getQuery();

                if (path.matches("/movies/[^/]+")) {
                    String id = path.substring(path.lastIndexOf("/") + 1);
                    getMovieById(ex, id);
                } else if (query != null) {
                    searchByYear(ex, query);
                } else {
                    getMovies(ex);
                    return;
                }
                break;
            }

            case "POST": {
                addMovie(ex);
                break;
            }

            case "DELETE": {
                if (path.matches("/movies/[^/]+")) {
                    String id = path.substring(path.lastIndexOf("/") + 1);
                    deleteMovieById(ex, id);
                } else {
                    sendBadRequest(ex);
                }
            }
        }
    }

    private void addMovie(HttpExchange ex) throws IOException {

        String contentType = ex.getRequestHeaders().getFirst("Content-Type");

        if (contentType == null || !contentType.startsWith("application/json")) {
            sendUnsupportedMediaType(ex);
            return;
        }

        MovieCreateRequest movieToCreate;

        try (InputStream is = ex.getRequestBody()) {
            String body = new String(
                    is.readAllBytes(),
                    StandardCharsets.UTF_8
            );

            movieToCreate = gson.fromJson(body, MovieCreateRequest.class);

            if (movieToCreate == null) {
                throw new IllegalArgumentException();
            }

            ArrayList<String> errorsArray = new ArrayList<>();

            if (movieToCreate.title.isBlank()) {
                errorsArray.add("название не должно быть пустым");
            }
            if (movieToCreate.title.length() > 100) {
                errorsArray.add("название слишком длинное");
            }
            if (movieToCreate.year < 1880 || movieToCreate.year > LocalDateTime.now().getYear() + 1) {
                errorsArray.add("год должен быть между 1888 и 2026");
            }
            if (!errorsArray.isEmpty()) {
                sendJson(ex, 422, gson.toJson(new ErrorResponse("Ошибка валидации", errorsArray)));
                return;
            }

            sendJson(ex, 201, moviesStore.saveMovie(movieToCreate.title, movieToCreate.year));


        } catch (JsonSyntaxException e) {
            sendBadRequest(ex);
        } catch (IOException e) {
            // Ошибка чтения тела запроса
            sendJson(ex, 500, gson.toJson(new ErrorResponse("Неизвестная ошибка")));
        }

    }

    private void getMovies(HttpExchange ex) throws IOException {
        sendJson(ex, 200, moviesStore.getAllMovies());
    }

    private void searchByYear(HttpExchange ex, String query) throws IOException {
        String[] params = query.split("&");

        for (String param : params) {
            String[] keyValue = param.split("=", 2);

            if (keyValue.length == 2 && keyValue[0].equals("year")) {

                try {
                    int year = Integer.parseInt(keyValue[1]);

                    if (year < 1888 || year > LocalDateTime.now().getYear() + 1) {
                        ErrorResponse error = new ErrorResponse("год должен быть между 1888 и 2026");
                        sendJson(ex, 400, gson.toJson(error));
                        return;
                    }
                    sendJson(ex, 200, moviesStore.searchByYear(year));

                } catch (NumberFormatException e) {
                    ErrorResponse error = new ErrorResponse("Неправильный формат параметра year");
                    sendJson(ex, 400, gson.toJson(error));
                }
                return;
            }
            sendBadRequest(ex);
        }

    }

    private void getMovieById(HttpExchange ex, String id) throws IOException {
        try {
            int intId = Integer.parseInt(id);

            Optional<String> optionalMovie = moviesStore.getById(intId);

            if (optionalMovie.isEmpty()) {
                sendNotFound(ex);
            } else {
                sendJson(ex, 200, optionalMovie.get());
            }
        } catch (NumberFormatException e) {
            ErrorResponse error = new ErrorResponse("Неправильный формат id");
            sendJson(ex, 400, gson.toJson(error));
        }
    }

    private void deleteMovieById(HttpExchange ex, String id) throws IOException {
        try {
            int intId = Integer.parseInt(id);

            boolean isDeleted = moviesStore.deleteMovieById(intId);

            if (isDeleted) {
                sendNoContent(ex);
            } else {
                sendNotFound(ex);
            }
        } catch (NumberFormatException e) {
            ErrorResponse error = new ErrorResponse("Неправильный формат id");
            sendJson(ex, 400, gson.toJson(error));
        }
    }
}

