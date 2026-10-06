package com.devtracker.support;

import com.devtracker.entities.User;
import com.devtracker.repositories.UserRepository;
import com.devtracker.services.UserService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Centralized identity resolver that extracts user email and resolves
 * accounts across all authentication mechanisms (Local Form, Google OAuth2/OIDC, GitHub OAuth2).
 */
public final class Helper {

    private Helper() {
    }

    /**
     * Resolves the verified email address of the logged-in user across all authentication providers
     * (Local/Form login, Google OAuth2/OIDC, GitHub OAuth2).
     *
     * @param authentication the current Spring Security Authentication token
     * @return normalized email address, or null if unauthenticated or email unavailable
     */
    public static String getEmailOfLoggedInUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        // 1. OIDC User (Google OpenID Connect)
        if (principal instanceof OidcUser oidcUser) {
            String oidcEmail = oidcUser.getEmail();
            if (StringUtils.hasText(oidcEmail)) {
                return EmailNormalizer.normalize(oidcEmail);
            }
        }

        // 2. OAuth2 User (Google, GitHub, or generic OAuth2 provider)
        if (principal instanceof OAuth2User oauth2User) {
            Object emailAttr = oauth2User.getAttribute("email");
            if (emailAttr instanceof String emailStr && StringUtils.hasText(emailStr)) {
                return EmailNormalizer.normalize(emailStr);
            }

            // GitHub fallback when email is private or hidden
            Object loginAttr = oauth2User.getAttribute("login");
            if (loginAttr != null && StringUtils.hasText(loginAttr.toString())) {
                return EmailNormalizer.normalize(loginAttr.toString() + "@users.noreply.github.com");
            }
        }

        // 3. UserDetails (standard Form / Local Login)
        if (principal instanceof UserDetails userDetails) {
            String username = userDetails.getUsername();
            if (StringUtils.hasText(username) && username.contains("@")) {
                return EmailNormalizer.normalize(username);
            }
        }

        // 4. Fallback to authentication.getName() if it resembles an email address
        String name = authentication.getName();
        if (StringUtils.hasText(name) && name.contains("@")) {
            return EmailNormalizer.normalize(name);
        }

        return null;
    }

    /**
     * Resolves the persisted User entity for the current authentication session using UserService.
     */
    public static Optional<User> getLoggedInUser(Authentication authentication, UserService userService) {
        String email = getEmailOfLoggedInUser(authentication);
        if (!StringUtils.hasText(email)) {
            return Optional.empty();
        }
        return userService.getUserByEmail(email);
    }

    /**
     * Resolves the persisted User entity for the current authentication session using UserRepository.
     */
    public static Optional<User> getLoggedInUser(Authentication authentication, UserRepository userRepository) {
        String email = getEmailOfLoggedInUser(authentication);
        if (!StringUtils.hasText(email)) {
            return Optional.empty();
        }
        return userRepository.findByEmailIgnoreCase(email);
    }
}
