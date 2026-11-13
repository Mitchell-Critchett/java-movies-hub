
package ru.practicum.moviehub.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class MoviesApiTest {

    private static final String BASE = "http://localhost:8080";

    private MoviesServer server;
    private HttpClient client;

    @BeforeEach
    void setUp() {
        server = new MoviesServer(new MoviesStore(), 8080);

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    // =========================
    // Тест кейсы эндпоинта
    // GET /movies
    // =========================

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpResponse<String> response = get("/movies");

        assertEquals(200, response.statusCode());

        JsonArray movies = JsonParser.parseString(response.body())
                .getAsJsonArray();

        assertEquals(0, movies.size());
    }

    @Test
    void getMovies_whenMoviesExist_returnsAllMovies() throws Exception {
        createMovie("Matrix", 1999);
        createMovie("Inception", 2010);

        HttpResponse<String> response = get("/movies");

        assertEquals(200, response.statusCode());

        JsonArray movies = JsonParser.parseString(response.body())
                .getAsJsonArray();

        assertEquals(2, movies.size());
    }

    @Test
    void getMovies_afterDelete_returnsUpdatedList() throws Exception {
        int firstId = createMovie("Matrix", 1999);
        createMovie("Inception", 2010);

        delete("/movies/" + firstId);

        HttpResponse<String> response = get("/movies");

        assertEquals(200, response.statusCode());

        JsonArray movies = JsonParser.parseString(response.body())
                .getAsJsonArray();

        assertEquals(1, movies.size());
    }

    // =========================
    // Тест кейсы эндпоинта
    // POST /movies
    // =========================

    @Test
    void addMovie_withValidData_returnsCreatedMovie() throws Exception {
        HttpResponse<String> response =
                post("/movies", """
                        {
                            "title": "Interstellar",
                            "year": 2014
                        }
                        """);

        assertEquals(201, response.statusCode());

        JsonObject movie = JsonParser.parseString(response.body())
                .getAsJsonObject();

        assertTrue(movie.has("id"));
        assertTrue(movie.get("id").getAsInt() > 0);

        assertEquals("Interstellar",
                movie.get("title").getAsString());

        assertEquals(2014,
                movie.get("year").getAsInt());
    }

    @Test
    void addMovie_withEmptyTitle_returnsValidationError() throws Exception {
        HttpResponse<String> response =
                post("/movies", """
                        {
                            "title": "",
                            "year": 2000
                        }
                        """);

        assertEquals(422, response.statusCode());

        JsonObject error = JsonParser.parseString(response.body())
                .getAsJsonObject();

        assertTrue(error.has("error"));
        assertTrue(error.has("details"));

        JsonArray details = error.getAsJsonArray("details");

        assertTrue(details.size() > 0);
    }

    @Test
    void addMovie_withMalformedJson_returnsBadRequest() throws Exception {
        HttpResponse<String> response =
                post("/movies", "{invalid json}");

        assertEquals(400, response.statusCode());
    }

    // =========================
    // Тест кейсы эндпоинта
    // GET /movies/{id}
    // =========================

    @Test
    void getMovieById_whenExists_returnsMovie() throws Exception {
        int id = createMovie("Interstellar", 2014);

        HttpResponse<String> response =
                get("/movies/" + id);

        assertEquals(200, response.statusCode());

        JsonObject movie = JsonParser.parseString(response.body())
                .getAsJsonObject();

        assertEquals(id, movie.get("id").getAsInt());
        assertEquals("Interstellar",
                movie.get("title").getAsString());
        assertEquals(2014,
                movie.get("year").getAsInt());
    }

    @Test
    void getMovieById_whenNotExists_returns404() throws Exception {
        HttpResponse<String> response =
                get("/movies/999999");

        assertEquals(404, response.statusCode());
    }

    @Test
    void getMovieById_withNonNumericId_returns400() throws Exception {
        HttpResponse<String> response =
                get("/movies/abc");

        assertEquals(400, response.statusCode());
    }

    // =========================
    // Тест кейсы эндпоинта
    // DELETE /movies/{id}
    // =========================

    @Test
    void deleteMovieById_whenExists_returns204() throws Exception {
        int id = createMovie("Matrix", 1999);

        HttpResponse<String> response =
                delete("/movies/" + id);

        assertEquals(204, response.statusCode());
        assertEquals("", response.body());
    }

    @Test
    void deleteMovieById_whenNotExists_returns404() throws Exception {
        HttpResponse<String> response =
                delete("/movies/999999");

        assertEquals(404, response.statusCode());
    }

    @Test
    void deleteMovieById_withNonNumericId_returns400() throws Exception {
        HttpResponse<String> response =
                delete("/movies/abc");

        assertEquals(400, response.statusCode());
    }

    // =========================
    // Тест кейсы эндпоинта
    // GET /movies?year=YYYY
    // =========================

    @Test
    void searchMoviesByYear_whenMatchesExist_returnsMovies() throws Exception {
        createMovie("Matrix", 1999);
        createMovie("Fight Club", 1999);
        createMovie("Inception", 2010);

        HttpResponse<String> response =
                get("/movies?year=1999");

        assertEquals(200, response.statusCode());

        JsonArray movies = JsonParser.parseString(response.body())
                .getAsJsonArray();

        assertEquals(2, movies.size());

        for (int i = 0; i < movies.size(); i++) {
            assertEquals(1999,
                    movies.get(i).getAsJsonObject()
                            .get("year").getAsInt());
        }
    }

    @Test
    void searchMoviesByYear_whenNoMatches_returnsEmptyArray() throws Exception {
        HttpResponse<String> response =
                get("/movies?year=2001");

        assertEquals(200, response.statusCode());

        JsonArray movies = JsonParser.parseString(response.body())
                .getAsJsonArray();

        assertEquals(0, movies.size());
    }

    @Test
    void searchMoviesByYear_withNonNumericYear_returns400() throws Exception {
        HttpResponse<String> response =
                get("/movies?year=abc");

        assertEquals(400, response.statusCode());
    }

    // =========================
    // Вспомогательные методы
    // =========================

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .GET()
                .build();

        return client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
    }

    private HttpResponse<String> post(
            String path,
            String json
    ) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        json,
                        StandardCharsets.UTF_8
                ))
                .build();

        return client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
    }

    private HttpResponse<String> delete(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .DELETE()
                .build();

        return client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
    }

    private int createMovie(String title, int year) throws Exception {
        HttpResponse<String> response = post("/movies", """
                {
                    "title": "%s",
                    "year": %d
                }
                """.formatted(title, year));

        assertEquals(201, response.statusCode(),
                "Не удалось создать тестовый фильм: " + response.body());

        JsonObject movie = JsonParser.parseString(response.body())
                .getAsJsonObject();

        return movie.get("id").getAsInt();
    }
}
