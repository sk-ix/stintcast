package io.github.skix.stintcast.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.skix.stintcast.model.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

/**
 * Client for the OpenF1 REST API (https://openf1.org).
 */
public class OpenF1Client {

    private static final String BASE_URL = "https://api.openf1.org/v1/";

    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    /**
     * Creates a client with a default HTTP client and JSON mapper.
     */
    public OpenF1Client() {
        this(HttpClient.newHttpClient(), defaultMapper());
    }

    /**
     * Creates a client with the given HTTP client and JSON mapper.
     */
    public OpenF1Client(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.mapper = objectMapper;
    }

    private static ObjectMapper defaultMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
        return mapper;
    }

    /**
     * Returns all laps of the given session.
     */
    public List<Lap> getLaps(int sessionKey) throws IOException, InterruptedException {
        return fetch("laps", "session_key=" + sessionKey, new TypeReference<>() {});
    }

    /**
     * Returns all stints of the given session.
     */
    public List<Stint> getStints(int sessionKey) throws IOException, InterruptedException {
        return fetch("stints", "session_key=" + sessionKey, new TypeReference<>() {});
    }

    /**
     * Returns all pit stops of the given session.
     */
    public List<Pit> getPits(int sessionKey) throws IOException, InterruptedException {
        return fetch("pit", "session_key=" + sessionKey, new TypeReference<>() {});
    }

    /**
     * Returns all race sessions of the given season (sprint races excluded).
     */
    public List<Session> getRaceSessions(int year) throws IOException, InterruptedException {
        return fetch("sessions", "year=" + year + "&session_name=Race", new TypeReference<>() {});
    }

    /**
     * Returns the safety car and virtual safety car messages of the given session.
     */
    public List<RaceControlMessage> getSafetyCarMessages(int sessionKey) throws IOException, InterruptedException {
        return fetch("race_control", "session_key=" + sessionKey + "&category=SafetyCar", new TypeReference<>() {});
    }

    private <T> List<T> fetch(String endpoint, String query, TypeReference<List<T>> type)
            throws IOException, InterruptedException {
        Thread.sleep(2000); // OpenF1 free tier: max 30 requests per minute
        String url = BASE_URL + endpoint + "?" + query;
        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        // OpenF1 returns 404 instead of an empty list when no record matches the filters
        if (response.statusCode() == 404 && response.body().contains("No results found")) return List.of();

        if (response.statusCode() != 200) throw new IOException("OpenF1 returned status " + response.statusCode() + ": " + response.body());

        return mapper.readValue(response.body(), type);
    }
}