package com.devtracker.serviceImplementation;

import com.devtracker.repositories.UserRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserServiceImplementationTest {

    @Test
    void getUserByEmailUsesNormalizedLookup() {
        String[] lookedUpEmail = new String[1];
        UserRepository repository = (UserRepository) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findByEmailIgnoreCase")) {
                        lookedUpEmail[0] = (String) args[0];
                        return Optional.empty();
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        UserServiceImplementation service = new UserServiceImplementation(repository);

        service.getUserByEmail("  Test@EXAMPLE.Com ");

        assertEquals("test@example.com", lookedUpEmail[0]);
    }
}
