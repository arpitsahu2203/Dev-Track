package com.devtracker.config;

import com.devtracker.entities.Providers;
import com.devtracker.entities.User;
import com.devtracker.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
        passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-random-password");

        handler = new OAuthAuthenticationSuccessHandler(userRepository, passwordEncoder);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void provisionsNewUserOnGoogleOAuthLogin() throws IOException, ServletException {
        Map<String, Object> attributes = Map.of(
                "sub", "google-sub-777",
                "email", "google.dev@example.com",
                "name", "Google Developer",
                "picture", "https://example.com/google.png"
        );
        OAuth2User oauth2User = new DefaultOAuth2User(Collections.emptyList(), attributes, "sub");
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                oauth2User, oauth2User.getAuthorities(), "google");

        when(userRepository.findByEmail("google.dev@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("google.dev@example.com")).thenReturn(Optional.empty());

        handler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertEquals("google.dev@example.com", saved.getEmail());
        assertEquals("Google Developer", saved.getName());
        assertEquals("https://example.com/google.png", saved.getProfilePic());
        assertEquals(Providers.GOOGLE, saved.getProvider());
        assertEquals("google-sub-777", saved.getProviderUserId());
        assertTrue(saved.isEmailVerified());
        assertTrue(saved.isEnabled());
        assertEquals("/devtracker/home", response.getRedirectedUrl());
    }

    @Test
    void provisionsNewUserOnGitHubOAuthLogin() throws IOException, ServletException {
        Map<String, Object> attributes = Map.of(
                "id", 998877,
                "email", "github.coder@domain.io",
                "name", "Octo Coder",
                "avatar_url", "https://avatars.github.com/u/998877"
        );
        OAuth2User oauth2User = new DefaultOAuth2User(Collections.emptyList(), attributes, "id");
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                oauth2User, oauth2User.getAuthorities(), "github");

        when(userRepository.findByEmail("github.coder@domain.io")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("github.coder@domain.io")).thenReturn(Optional.empty());

        handler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertEquals("github.coder@domain.io", saved.getEmail());
        assertEquals("Octo Coder", saved.getName());
        assertEquals("https://avatars.github.com/u/998877", saved.getProfilePic());
        assertEquals(Providers.GITHUB, saved.getProvider());
        assertEquals("998877", saved.getProviderUserId());
        assertTrue(saved.isEmailVerified());
        assertEquals("/devtracker/home", response.getRedirectedUrl());
    }

    @Test
    void mergesAccountWhenUserAlreadyExistsFromFormOrAnotherProvider() throws IOException, ServletException {
        User existingUser = User.builder()
                .email("shared.dev@example.com")
                .name("Local Engineer")
                .password("$2a$10$existingPasswordHash")
                .enabled(true)
                .emailVerified(false)
                .provider(Providers.LOCAL)
                .providerUserId(null)
                .profilePic("https://example.com/old-avatar.png")
                .roleList(List.of("ROLE_USER"))
                .build();

        when(userRepository.findByEmail("shared.dev@example.com")).thenReturn(Optional.of(existingUser));

        Map<String, Object> githubAttributes = Map.of(
                "id", 112233,
                "email", "shared.dev@example.com",
                "name", "GitHub Updated Name",
                "avatar_url", "https://avatars.github.com/u/112233"
        );
        OAuth2User oauth2User = new DefaultOAuth2User(Collections.emptyList(), githubAttributes, "id");
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                oauth2User, oauth2User.getAuthorities(), "github");

        handler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User updated = captor.getValue();
        // Check account unification: same email, preserved password, merged OAuth details
        assertEquals("shared.dev@example.com", updated.getEmail());
        assertEquals("$2a$10$existingPasswordHash", updated.getPassword());
        assertEquals("https://avatars.github.com/u/112233", updated.getProfilePic());
        assertEquals(Providers.GITHUB, updated.getProvider());
        assertEquals("112233", updated.getProviderUserId());
        assertTrue(updated.isEmailVerified());
        assertEquals("/devtracker/home", response.getRedirectedUrl());
    }

    @Test
    void redirectsToOAuthErrorWhenEmailCannotBeDetermined() throws IOException, ServletException {
        Map<String, Object> attributes = Map.of(
                "custom_id", "none"
        );
        OAuth2User oauth2User = new DefaultOAuth2User(Collections.emptyList(), attributes, "custom_id");
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                oauth2User, oauth2User.getAuthorities(), "custom_oauth");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(userRepository, never()).save(any());
        assertEquals("/login?oauthError", response.getRedirectedUrl());
    }
}
