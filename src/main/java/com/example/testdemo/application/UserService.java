package com.example.testdemo.application;

import com.example.testdemo.domain.User;
import com.example.testdemo.domain.UserNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * The application logic. It sits in the middle of the hexagon and only talks to
 * ports, never to a controller or a database.
 */
@Service
public class UserService implements UserUseCase {

    private final UserRepositoryPort repository;

    public UserService(UserRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public User create(String name, String email) {
        // id is null here, the repository assigns one
        return repository.save(new User(null, name, email));
    }

    @Override
    public User findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    @Override
    public List<User> findAll() {
        return repository.findAll();
    }

    @Override
    public User update(Long id, String name, String email) {
        findById(id); // fails if the user does not exist
        return repository.save(new User(id, name, email));
    }

    @Override
    public void delete(Long id) {
        findById(id); // fails if the user does not exist
        repository.deleteById(id);
    }
}
