package com.example.testdemo.domain;

/** Thrown when we ask for a user that does not exist. */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long id) {
        super("User not found: " + id);
    }
}
