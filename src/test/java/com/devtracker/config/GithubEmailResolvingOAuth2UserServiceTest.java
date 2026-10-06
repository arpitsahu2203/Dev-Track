package com.devtracker.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GithubEmailResolvingOAuth2UserServiceTest {

    private MockRestServiceServer mockServer;
    private GithubEmailResolvingOAuth2UserService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        service = new GithubEmailResolvingOAuth2UserService(request -> null, builder.build());
    }

    @Test
    void fetchGithubEmailReturnsPrimaryVerifiedEmail() {
        String json = """
                [
                    {"email": "secondary@domain.com", "primary": false, "verified": true},
                    {"email": "primary@domain.com", "primary": true, "verified": true},
                    {"email": "unverified@domain.com", "primary": false, "verified": false}
                ]
                """;

        mockServer.expect(requestTo("https://api.github.com/user/emails"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer gh-token-123"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        String email = service.fetchGithubEmail("gh-token-123");

        assertEquals("primary@domain.com", email);
        mockServer.verify();
    }

    @Test
    void fetchGithubEmailConvertsGitHubErrorsToOAuthFailure() {
        mockServer.expect(requestTo("https://api.github.com/user/emails"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        assertThrows(OAuth2AuthenticationException.class, () -> service.fetchGithubEmail("token"));
        mockServer.verify();
    }
}
