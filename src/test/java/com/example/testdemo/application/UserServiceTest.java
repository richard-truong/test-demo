package com.example.testdemo.application;

import com.example.testdemo.domain.User;
import com.example.testdemo.domain.UserNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;

/**
 * UNIT TEST
 *
 * <p>We test the business logic on its own: the repository is a mock, so there
 * is no Spring, no database and no HTTP. Fast, and it fails for exactly one
 * reason when it fails.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepositoryPort repository;

    @InjectMocks
    private UserService service;

    @Test
    @DisplayName("create() saves the user and returns the saved version")
    void create_savesUser() {
        given(repository.save(any())).willReturn(new User(1L, "Alice", "alice@example.com"));

        User created = service.create("Alice", "alice@example.com");

        assertThat(created.id()).isEqualTo(1L);
        assertThat(created.name()).isEqualTo("Alice");
        // the new user has no id yet, the repository is the one assigning it
        then(repository).should().save(new User(null, "Alice", "alice@example.com"));
    }

    @Test
    @DisplayName("findById() returns the user when it exists")
    void findById_returnsUser() {
        given(repository.findById(1L)).willReturn(Optional.of(new User(1L, "Alice", "alice@example.com")));

        User found = service.findById(1L);

        assertThat(found.name()).isEqualTo("Alice");
    }

    @Test
    @DisplayName("findById() throws when the user does not exist")
    void findById_throwsWhenMissing() {
        given(repository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("findAll() returns every user")
    void findAll_returnsAllUsers() {
        given(repository.findAll()).willReturn(List.of(
                new User(1L, "Alice", "alice@example.com"),
                new User(2L, "Bob", "bob@example.com")));

        assertThat(service.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("update() overwrites the name and email")
    void update_changesFields() {
        given(repository.findById(1L)).willReturn(Optional.of(new User(1L, "Alice", "alice@example.com")));
        given(repository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        User updated = service.update(1L, "Bob", "bob@example.com");

        assertThat(updated.name()).isEqualTo("Bob");
        assertThat(updated.email()).isEqualTo("bob@example.com");
        then(repository).should().save(new User(1L, "Bob", "bob@example.com"));
    }

    @Test
    @DisplayName("update() throws and saves nothing when the user does not exist")
    void update_throwsWhenMissing() {
        given(repository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, "Bob", "bob@example.com"))
                .isInstanceOf(UserNotFoundException.class);

        then(repository).should(never()).save(any());
    }

    @Test
    @DisplayName("delete() removes the user")
    void delete_removesUser() {
        given(repository.findById(1L)).willReturn(Optional.of(new User(1L, "Alice", "alice@example.com")));

        service.delete(1L);

        then(repository).should().deleteById(1L);
    }

    @Test
    @DisplayName("delete() throws when the user does not exist")
    void delete_throwsWhenMissing() {
        given(repository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(UserNotFoundException.class);

        then(repository).should(never()).deleteById(any());
    }
}
