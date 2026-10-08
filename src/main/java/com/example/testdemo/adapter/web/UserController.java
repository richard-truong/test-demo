package com.example.testdemo.adapter.web;

import com.example.testdemo.application.UserUseCase;
import com.example.testdemo.domain.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Input adapter: exposes the use cases over HTTP.
 *
 * <p>It only knows {@link UserUseCase}. It has no idea that users are kept in
 * memory, and it contains no business logic.
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserUseCase userUseCase;

    public UserController(UserUseCase userUseCase) {
        this.userUseCase = userUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User create(@RequestBody UserRequest request) {
        return userUseCase.create(request.name(), request.email());
    }

    @GetMapping("/{id}")
    public User findById(@PathVariable Long id) {
        return userUseCase.findById(id);
    }

    @GetMapping
    public List<User> findAll() {
        return userUseCase.findAll();
    }

    @PutMapping("/{id}")
    public User update(@PathVariable Long id, @RequestBody UserRequest request) {
        return userUseCase.update(id, request.name(), request.email());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        userUseCase.delete(id);
    }
}
