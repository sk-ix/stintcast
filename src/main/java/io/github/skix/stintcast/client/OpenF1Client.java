package io.github.skix.stintcast.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.skix.stintcast.model.Lap;
import io.github.skix.stintcast.model.Pit;
import io.github.skix.stintcast.model.Stint;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class OpenF1Client {

    private static final String BASE_URL = "https://api.openf1.org/v1/";

    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public OpenF1Client() {
        this(HttpClient.newHttpClient(), defaultMapper());
    }

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

    public List<Lap> getLaps(int sessionKey) throws IOException, InterruptedException { return fetch("laps", sessionKey, new TypeReference<>() {}); }

    public List<Stint> getStints(int sessionKey) throws IOException, InterruptedException { return fetch("stints", sessionKey, new TypeReference<>() {}); }

    public List<Pit> getPits(int sessionKey) throws IOException, InterruptedException { return fetch("pit", sessionKey, new TypeReference<>() {}); }

    private <T> List<T> fetch(String endpoint, int sessionKey, TypeReference<List<T>> type)
            throws IOException, InterruptedException {
        String url = BASE_URL + endpoint + "?session_key=" + sessionKey;
        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IOException("OpenF1 returned status " + response.statusCode() + ": " + response.body());
        return mapper.readValue(response.body(), type);
    }
}
