package com.example.testdemo.application;

import com.example.testdemo.domain.User;

import java.util.List;
import java.util.Optional;

/**
 * Output port: what our application needs from storage.
 *
 * <p>The application only knows this interface. Whether the data ends up in a
 * HashMap, a Postgres table or a file is the adapter's problem.
 */
public interface UserRepositoryPort {

    /** Stores the user and returns the saved version (with an id filled in). */
    User save(User user);

    Optional<User> findById(Long id);

    List<User> findAll();

    void deleteById(Long id);
}
