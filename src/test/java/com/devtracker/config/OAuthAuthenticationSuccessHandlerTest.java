package com.devtracker.config;

import com.devtracker.entities.Providers;
import com.devtracker.entities.User;
import com.devtracker.repositories.UserRepository;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OAuthAuthenticationSuccessHandlerTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private OAuthAuthenticationSuccessHandler handler;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = NoOpPasswordEncoder.getInstance();
        handler = new OAuthAuthenticationSuccessHandler(userRepository, passwordEncoder);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void provisionsNewUserOnGoogleLogin() throws IOException, ServletException {
        Map<String, Object> attributes = Map.of(
                "sub", "google-sub-777",
                "email", "Alice@Gmail.com",
                "name", "Alice Cooper",
                "picture", "https://google.com/alice.jpg"
        );
        OAuth2User oauth2User = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")), attributes, "sub");
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                oauth2User, oauth2User.getAuthorities(), "google");

        when(userRepository.findByEmail("alice@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("alice@gmail.com")).thenReturn(Optional.empty());

        handler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertEquals("alice@gmail.com", saved.getEmail());
        assertEquals("Alice Cooper", saved.getName());
        assertEquals(Providers.GOOGLE, saved.getProvider());
        assertEquals("google-sub-777", saved.getProviderId());
        assertEquals("https://google.com/alice.jpg", saved.getProfilePic());
        assertTrue(saved.isEmailVerified());
        assertTrue(saved.isEnabled());
        assertTrue(saved.getRoleList().contains("ROLE_USER"));
        assertNotNull(saved.getPassword());
        assertEquals("/devtracker/home", response.getRedirectedUrl());
    }

    @Test
    void provisionsNewUserOnGitHubLoginWithHiddenEmailFallback() throws IOException, ServletException {
        Map<String, Object> attributes = Map.of(
                "id", 998877,
                "login", "dev-octo",
                "avatar_url", "https://github.com/avatar.png"
        );
        OAuth2User oauth2User = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")), attributes, "login");
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                oauth2User, oauth2User.getAuthorities(), "github");

        String expectedFallbackEmail = "dev-octo@users.noreply.github.com";
        when(userRepository.findByEmail(expectedFallbackEmail)).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase(expectedFallbackEmail)).thenReturn(Optional.empty());

        handler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertEquals(expectedFallbackEmail, saved.getEmail());
        assertEquals("dev-octo", saved.getName());
        assertEquals(Providers.GITHUB, saved.getProvider());
        assertEquals("998877", saved.getProviderId());
        assertEquals("https://github.com/avatar.png", saved.getProfilePic());
        assertTrue(saved.isEmailVerified());
        assertEquals("/devtracker/home", response.getRedirectedUrl());
    }

    @Test
    void mergesAccountWhenUserAlreadyExistsFromFormOrAnotherProvider() throws IOException, ServletException {
        // User originally registered locally with password
        User existingUser = User.builder()
                .email("charlie@domain.com")
                .name("Charlie Original")
                .password("bcrypt-hashed-secret-password")
                .phoneNumber("9876543210")
                .provider(Providers.SELF)
                .emailVerified(false)
                .enabled(true)
                .build();

        when(userRepository.findByEmail("charlie@domain.com")).thenReturn(Optional.of(existingUser));

        // Now logging in via GitHub with the same verified email
        Map<String, Object> attributes = Map.of(
                "id", 112233,
                "login", "charlie-dev",
                "email", "charlie@domain.com",
                "avatar_url", "https://github.com/charlie.png"
        );
        OAuth2User oauth2User = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")), attributes, "login");
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                oauth2User, oauth2User.getAuthorities(), "github");

        handler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User updated = captor.getValue();

        // Check account unification: same email, preserved password, merged OAuth details
        assertEquals("charlie@domain.com", updated.getEmail());
        assertEquals("Charlie Original", updated.getName());
        assertEquals("bcrypt-hashed-secret-password", updated.getPassword());
        assertEquals(Providers.GITHUB, updated.getProvider());
        assertEquals("112233", updated.getProviderId());
        assertEquals("https://github.com/charlie.png", updated.getProfilePic());
        assertTrue(updated.isEmailVerified());
        assertEquals("/devtracker/home", response.getRedirectedUrl());
    }

    @Test
    void redirectsToOAuthErrorWhenEmailCannotBeDetermined() throws IOException, ServletException {
        Map<String, Object> attributes = new HashMap<>();
        // No email, no login attribute
        attributes.put("id", 12345);

        OAuth2User oauth2User = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")), attributes, "id");
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                oauth2User, oauth2User.getAuthorities(), "custom_oauth");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(userRepository, never()).save(any());
        assertEquals("/login?oauthError", response.getRedirectedUrl());
    }
}
