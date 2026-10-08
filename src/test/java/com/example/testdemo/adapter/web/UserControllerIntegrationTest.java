package com.example.testdemo.adapter.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * INTEGRATION TEST
 *
 * <p>Nothing is mocked here. This starts the real Spring application and sends
 * real HTTP requests through the whole hexagon: controller -> use case ->
 * repository adapter. It catches problems the unit test cannot see, such as a
 * wrong URL, broken JSON mapping or a bean that was not wired.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("POST /users creates a user and returns 201 with the generated id")
    void createUser() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Alice", "email": "alice@example.com"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    @DisplayName("GET /users/{id} returns the user")
    void findUserById() throws Exception {
        long id = createUser("Alice", "alice@example.com");

        mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Alice"));
    }

    @Test
    @DisplayName("GET /users/{id} returns 404 when the user does not exist")
    void findUserById_returnsNotFound() throws Exception {
        mockMvc.perform(get("/users/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found: 999999"));
    }

    @Test
    @DisplayName("GET /users returns the created users")
    void findAllUsers() throws Exception {
        createUser("Carol", "carol@example.com");

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", hasItem("carol@example.com")));
    }

    @Test
    @DisplayName("PUT /users/{id} updates the user")
    void updateUser() throws Exception {
        long id = createUser("Dave", "dave@example.com");

        mockMvc.perform(put("/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Dave Updated", "email": "dave.new@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Dave Updated"))
                .andExpect(jsonPath("$.email").value("dave.new@example.com"));

        mockMvc.perform(get("/users/{id}", id))
                .andExpect(jsonPath("$.name").value("Dave Updated"));
    }

    @Test
    @DisplayName("DELETE /users/{id} removes the user")
    void deleteUser() throws Exception {
        long id = createUser("Erin", "erin@example.com");

        mockMvc.perform(delete("/users/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isNotFound());
    }

    /** Creates a user over HTTP and returns the id the server assigned. */
    private long createUser(String name, String email) throws Exception {
        String body = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "email": "%s"}
                                """.formatted(name, email)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
