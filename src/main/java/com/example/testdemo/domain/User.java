package com.example.testdemo.domain;

/**
 * The user of our system.
 *
 * <p>Pure business object: no framework, no database, no JSON annotations.
 * It only knows about itself.
 */
public record User(Long id, String name, String email) {
}
