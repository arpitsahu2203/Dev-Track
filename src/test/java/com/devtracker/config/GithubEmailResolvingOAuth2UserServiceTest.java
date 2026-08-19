package com.devtracker.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GithubEmailResolvingOAuth2UserServiceTest {

    private MockRestServiceServer server;
    private GithubEmailResolvingOAuth2UserService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new GithubEmailResolvingOAuth2UserService(request -> null, builder.build());
    }

    @Test
    void fetchGithubEmailPrefersPrimaryVerifiedEmail() {
        expectEmails("[{\"email\":\"other@example.com\",\"primary\":false,\"verified\":true}," +
                "{\"email\":\"primary@example.com\",\"primary\":true,\"verified\":true}]");

        assertEquals("primary@example.com", service.fetchGithubEmail("token"));
        server.verify();
    }

    @Test
    void fetchGithubEmailUsesAnyVerifiedEmailWhenNoPrimaryExists() {
        expectEmails("[{\"email\":\"verified@example.com\",\"primary\":false,\"verified\":true}]");

        assertEquals("verified@example.com", service.fetchGithubEmail("token"));
        server.verify();
    }

    @Test
    void fetchGithubEmailReturnsNullWhenNoVerifiedEmailExists() {
        expectEmails("[{\"email\":\"unverified@example.com\",\"primary\":true,\"verified\":false}]");

        assertNull(service.fetchGithubEmail("token"));
        server.verify();
    }

    @Test
    void fetchGithubEmailConvertsGitHubErrorsToOAuthFailure() {
        server.expect(requestTo("https://api.github.com/user/emails"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThrows(OAuth2AuthenticationException.class, () -> service.fetchGithubEmail("token"));
        server.verify();
    }

    private void expectEmails(String response) {
        server.expect(requestTo("https://api.github.com/user/emails"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer token"))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
    }
}
