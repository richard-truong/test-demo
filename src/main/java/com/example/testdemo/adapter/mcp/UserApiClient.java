package com.example.testdemo.adapter.mcp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Talks to this application's own REST API over HTTP.
 *
 * <p>This is what makes the MCP server a wrapper around the existing API rather
 * than a bypass of it: every MCP tool call becomes a real HTTP request to
 * {@code /users}.
 */
@Component
public class UserApiClient {

    private final RestClient restClient;

    public UserApiClient(@Value("${app.user-api.base-url:http://localhost:8080}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public String create(String name, String email) {
        return restClient.post()
                .uri("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("name", name, "email", email))
                .retrieve()
                .body(String.class);
    }

    public String findById(Long id) {
        return restClient.get()
                .uri("/users/{id}", id)
                .retrieve()
                .body(String.class);
    }

    public String findAll() {
        return restClient.get()
                .uri("/users")
                .retrieve()
                .body(String.class);
    }

    public String update(Long id, String name, String email) {
        return restClient.put()
                .uri("/users/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("name", name, "email", email))
                .retrieve()
                .body(String.class);
    }

    public void delete(Long id) {
        restClient.delete()
                .uri("/users/{id}", id)
                .retrieve()
                .toBodilessEntity();
    }
}
