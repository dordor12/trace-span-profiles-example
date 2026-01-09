package com.example.demo.service;

import com.example.demo.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class UserService {

    private final Map<Long, User> users = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Tracer tracer = GlobalOpenTelemetry.getTracer("UserService");

    public UserService() {
        createUser(new User(null, "John Doe", "john@example.com"));
        createUser(new User(null, "Jane Smith", "jane@example.com"));
        createUser(new User(null, "Bob Wilson", "bob@example.com"));
    }

    public List<User> getAllUsers() {
        Span span = tracer.spanBuilder("UserService.getAllUsers").startSpan();
        try (Scope scope = span.makeCurrent()) {
            validateRequest();
            List<User> userList = new ArrayList<>(users.values());
            List<User> result = processUserList(userList);
            auditAccess("getAllUsers", userList.size());
            return result;
        } finally {
            span.end();
        }
    }

    public Optional<User> getUserById(Long id) {
        Span span = tracer.spanBuilder("UserService.getUserById").setAttribute("user.id", id).startSpan();
        try (Scope scope = span.makeCurrent()) {
            validateRequest();
            Optional<User> user = Optional.ofNullable(users.get(id));
            user.ifPresent(u -> {
                enrichUserData(u);
                auditAccess("getUserById", 1);
            });
            return user;
        } finally {
            span.end();
        }
    }

    public User createUser(User user) {
        Span span = tracer.spanBuilder("UserService.createUser").startSpan();
        try (Scope scope = span.makeCurrent()) {
            validateUserData(user);
            Long id = idGenerator.getAndIncrement();
            user.setId(id);
            String serialized = serializeUser(user);
            User deserialized = deserializeUser(serialized);
            users.put(id, deserialized);
            auditAccess("createUser", 1);
            return deserialized;
        } finally {
            span.end();
        }
    }

    public Optional<User> updateUser(Long id, User user) {
        Span span = tracer.spanBuilder("UserService.updateUser").setAttribute("user.id", id).startSpan();
        try (Scope scope = span.makeCurrent()) {
            validateUserData(user);
            if (users.containsKey(id)) {
                user.setId(id);
                String serialized = serializeUser(user);
                User deserialized = deserializeUser(serialized);
                users.put(id, deserialized);
                auditAccess("updateUser", 1);
                return Optional.of(deserialized);
            }
            return Optional.empty();
        } finally {
            span.end();
        }
    }

    public boolean deleteUser(Long id) {
        Span span = tracer.spanBuilder("UserService.deleteUser").setAttribute("user.id", id).startSpan();
        try (Scope scope = span.makeCurrent()) {
            validateRequest();
            boolean removed = users.remove(id) != null;
            if (removed) {
                auditAccess("deleteUser", 1);
            }
            return removed;
        } finally {
            span.end();
        }
    }

    private void validateRequest() {
        Span span = tracer.spanBuilder("UserService.validateRequest").startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(2_000_000, 6_000_000);
        } finally {
            span.end();
        }
    }

    private void validateUserData(User user) {
        Span span = tracer.spanBuilder("UserService.validateUserData").startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(3_000_000, 8_000_000);
        } finally {
            span.end();
        }
    }

    private List<User> processUserList(List<User> users) {
        Span span = tracer.spanBuilder("UserService.processUserList").startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(4_000_000, 10_000_000);
            List<User> processed = new ArrayList<>();
            for (User user : users) {
                String json = serializeUser(user);
                processed.add(deserializeUser(json));
            }
            return processed;
        } finally {
            span.end();
        }
    }

    private void enrichUserData(User user) {
        Span span = tracer.spanBuilder("UserService.enrichUserData").startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(2_500_000, 7_000_000);
        } finally {
            span.end();
        }
    }

    private String serializeUser(User user) {
        Span span = tracer.spanBuilder("UserService.serializeUser").startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(1_500_000, 4_000_000);
            return objectMapper.writeValueAsString(user);
        } catch (JsonProcessingException e) {
            span.recordException(e);
            throw new RuntimeException("Serialization failed", e);
        } finally {
            span.end();
        }
    }

    private User deserializeUser(String json) {
        Span span = tracer.spanBuilder("UserService.deserializeUser").startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(1_500_000, 4_000_000);
            return objectMapper.readValue(json, User.class);
        } catch (JsonProcessingException e) {
            span.recordException(e);
            throw new RuntimeException("Deserialization failed", e);
        } finally {
            span.end();
        }
    }

    private void auditAccess(String operation, int count) {
        Span span = tracer.spanBuilder("UserService.auditAccess")
                .setAttribute("operation", operation)
                .setAttribute("count", count)
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            cpuWork(1_000_000, 3_000_000);
        } finally {
            span.end();
        }
    }

    private void cpuWork(int minIterations, int maxIterations) {
        int iterations = ThreadLocalRandom.current().nextInt(minIterations, maxIterations);
        double sum = 0;
        for (int i = 0; i < iterations; i++) {
            sum += Math.sqrt(i) * Math.sin(i) * Math.cos(i * 0.1);
            if (i % 1000 == 0) {
                sum = Math.log(Math.abs(sum) + 1);
            }
        }
    }
}
