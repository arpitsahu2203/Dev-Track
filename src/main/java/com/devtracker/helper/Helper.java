package com.devtracker.helper;

import com.devtracker.entities.User;
import com.devtracker.repositories.UserRepository;
import com.devtracker.services.UserService;
import org.springframework.security.core.Authentication;

import java.util.Optional;

/**
 * Facade helper in com.devtracker.helper forwarding to com.devtracker.support.Helper.
 */
public final class Helper {

    private Helper() {
    }

    public static String getEmailOfLoggedInUser(Authentication authentication) {
        return com.devtracker.support.Helper.getEmailOfLoggedInUser(authentication);
    }

    public static Optional<User> getLoggedInUser(Authentication authentication, UserService userService) {
        return com.devtracker.support.Helper.getLoggedInUser(authentication, userService);
    }

    public static Optional<User> getLoggedInUser(Authentication authentication, UserRepository userRepository) {
        return com.devtracker.support.Helper.getLoggedInUser(authentication, userRepository);
    }
}
