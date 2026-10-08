package com.example.testdemo.adapter.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

import java.util.function.Supplier;

/**
 * The MCP tools an LLM client can call.
 *
 * <p>Each method simply forwards to the REST API through {@link UserApiClient}.
 * Results are returned as JSON text, and HTTP errors are turned into a readable
 * message instead of an exception so the model can react to them.
 */
@Component
public class UserMcpTools {

    private final UserApiClient api;

    public UserMcpTools(UserApiClient api) {
        this.api = api;
    }

    @Tool(name = "create_user", description = "Create a new user. Returns the created user as JSON, including its generated id.")
    public String createUser(
            @ToolParam(description = "The user's full name") String name,
            @ToolParam(description = "The user's email address") String email) {
        return safely(() -> api.create(name, email));
    }

    @Tool(name = "get_user", description = "Fetch a single user by its id. Returns the user as JSON.")
    public String getUser(
            @ToolParam(description = "The id of the user") Long id) {
        return safely(() -> api.findById(id));
    }

    @Tool(name = "list_users", description = "List every user. Returns a JSON array with all users.")
    public String listUsers() {
        return safely(api::findAll);
    }

    @Tool(name = "update_user", description = "Replace the name and email of an existing user. Returns the updated user as JSON.")
    public String updateUser(
            @ToolParam(description = "The id of the user to update") Long id,
            @ToolParam(description = "The new full name") String name,
            @ToolParam(description = "The new email address") String email) {
        return safely(() -> api.update(id, name, email));
    }

    @Tool(name = "delete_user", description = "Delete a user by its id.")
    public String deleteUser(
            @ToolParam(description = "The id of the user to delete") Long id) {
        return safely(() -> {
            api.delete(id);
            return "User " + id + " deleted.";
        });
    }

    /** Runs the HTTP call and turns a 4xx/5xx into a message the model can read. */
    private String safely(Supplier<String> call) {
        try {
            return call.get();
        } catch (RestClientResponseException e) {
            return "Request failed with status " + e.getStatusCode().value()
                   + ": " + e.getResponseBodyAsString();
        }
    }
}
