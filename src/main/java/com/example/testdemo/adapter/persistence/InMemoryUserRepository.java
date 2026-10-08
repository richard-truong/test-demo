package com.example.testdemo.adapter.persistence;

import com.example.testdemo.application.UserRepositoryPort;
import com.example.testdemo.domain.User;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Output adapter: stores users in memory.
 *
 * <p>Swapping this for a real database means writing another class that
 * implements {@link UserRepositoryPort}. Nothing else in the app changes.
 */
@Repository
public class InMemoryUserRepository implements UserRepositoryPort {

    private final Map<Long, User> users = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong();

    @Override
    public User save(User user) {
        Long id = (user.id() != null) ? user.id() : nextId.incrementAndGet();
        User saved = new User(id, user.name(), user.email());
        users.put(id, saved);
        return saved;
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public List<User> findAll() {
        return List.copyOf(users.values());
    }

    @Override
    public void deleteById(Long id) {
        users.remove(id);
    }
}
