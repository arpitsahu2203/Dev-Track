package com.devtracker.support;

import com.devtracker.entities.User;
import com.devtracker.repositories.UserRepository;
import com.devtracker.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HelperTest {

    @Test
    void getEmailOfLoggedInUserReturnsNullForNullOrUnauthenticated() {
        assertNull(Helper.getEmailOfLoggedInUser(null));

        Authentication unauthenticated = mock(Authentication.class);
        when(unauthenticated.isAuthenticated()).thenReturn(false);
        assertNull(Helper.getEmailOfLoggedInUser(unauthenticated));

        AnonymousAuthenticationToken anonymous = new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        assertNull(Helper.getEmailOfLoggedInUser(anonymous));
    }

    @Test
    void getEmailOfLoggedInUserResolvesFormLoginUserDetails() {
        org.springframework.security.core.userdetails.User userDetails =
                new org.springframework.security.core.userdetails.User(
                        "DevUser@Example.COM", "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, "password", userDetails.getAuthorities());

        assertEquals("devuser@example.com", Helper.getEmailOfLoggedInUser(authentication));
    }

    @Test
    void getEmailOfLoggedInUserResolvesOidcUserEmail() {
        Map<String, Object> claims = Map.of(
                "sub", "google-sub-12345",
                "email", "GOOGLE.USER@GMAIL.COM",
                "email_verified", true
        );
        OidcIdToken idToken = new OidcIdToken("token-value", Instant.now(), Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken, "email");

        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                oidcUser, oidcUser.getAuthorities(), "google");

        assertEquals("google.user@gmail.com", Helper.getEmailOfLoggedInUser(authentication));
    }

    @Test
    void getEmailOfLoggedInUserResolvesOAuth2UserEmailAttribute() {
        Map<String, Object> attributes = Map.of(
                "id", 123456,
                "email", "GITHUB.USER@DOMAIN.IO",
                "login", "octocat"
        );
        OAuth2User oauth2User = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")), attributes, "login");

        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                oauth2User, oauth2User.getAuthorities(), "github");

        assertEquals("github.user@domain.io", Helper.getEmailOfLoggedInUser(authentication));
    }

    @Test
    void getEmailOfLoggedInUserResolvesGitHubFallbackWhenEmailIsNull() {
        Map<String, Object> attributes = Map.of(
                "id", 987654,
                "login", "DevCoder42"
        );
        OAuth2User oauth2User = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")), attributes, "login");

        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                oauth2User, oauth2User.getAuthorities(), "github");

        assertEquals("devcoder42@users.noreply.github.com", Helper.getEmailOfLoggedInUser(authentication));
    }

    @Test
    void getLoggedInUserDelegatesToUserServiceAndUserRepository() {
        org.springframework.security.core.userdetails.User userDetails =
                new org.springframework.security.core.userdetails.User(
                        "engineer@domain.com", "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, "n/a", userDetails.getAuthorities());

        User expectedUser = User.builder().email("engineer@domain.com").name("Engineer").build();

        UserService userService = mock(UserService.class);
        when(userService.getUserByEmail("engineer@domain.com")).thenReturn(Optional.of(expectedUser));

        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.findByEmailIgnoreCase("engineer@domain.com")).thenReturn(Optional.of(expectedUser));

        Optional<User> fromService = Helper.getLoggedInUser(auth, userService);
        assertTrue(fromService.isPresent());
        assertEquals("engineer@domain.com", fromService.get().getEmail());

        Optional<User> fromRepo = Helper.getLoggedInUser(auth, userRepository);
        assertTrue(fromRepo.isPresent());
        assertEquals("engineer@domain.com", fromRepo.get().getEmail());
    }
}
