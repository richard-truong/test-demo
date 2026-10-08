package com.example.testdemo.application;

import com.example.testdemo.domain.User;

import java.util.List;

/**
 * Input port: what the outside world may ask our application to do.
 *
 * <p>The web adapter talks to this interface, so it never depends on the
 * service implementation.
 */
public interface UserUseCase {

    User create(String name, String email);

    User findById(Long id);

    List<User> findAll();

    User update(Long id, String name, String email);

    void delete(Long id);
}
