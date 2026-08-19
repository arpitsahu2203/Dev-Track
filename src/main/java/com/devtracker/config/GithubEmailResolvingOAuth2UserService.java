package com.devtracker.config;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GithubEmailResolvingOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private static final String GITHUB_REGISTRATION_ID = "github";
    private static final String GITHUB_EMAILS_ENDPOINT = "https://api.github.com/user/emails";

    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;
    private final RestClient restClient;

    public GithubEmailResolvingOAuth2UserService() {
        this(new DefaultOAuth2UserService(), RestClient.create());
    }

    GithubEmailResolvingOAuth2UserService(
            OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate,
            RestClient restClient
    ) {
        this.delegate = delegate;
        this.restClient = restClient;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        if (!GITHUB_REGISTRATION_ID.equalsIgnoreCase(registrationId)) {
            return oauth2User;
        }

        String accessToken = userRequest.getAccessToken().getTokenValue();
        String githubEmail = fetchGithubEmail(accessToken);

        Map<String, Object> attributes = new LinkedHashMap<>(oauth2User.getAttributes());
        if (StringUtils.hasText(githubEmail)) {
            attributes.put("email", githubEmail);
            attributes.put("email_verified", true);
        } else {
            // Never auto-provision from a GitHub username or an unverified email.
            attributes.remove("email");
            attributes.put("email_verified", false);
        }

        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();
        if (!StringUtils.hasText(userNameAttributeName)) {
            userNameAttributeName = "id";
        }

        return new DefaultOAuth2User(oauth2User.getAuthorities(), attributes, userNameAttributeName);
    }

    String fetchGithubEmail(String accessToken) {
        List<GithubEmailRecord> emails;
        try {
            emails = restClient.get()
                    .uri(GITHUB_EMAILS_ENDPOINT)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (RestClientException ex) {
            OAuth2Error error = new OAuth2Error("github_email_lookup_failed", "Failed to retrieve email from GitHub.", null);
            throw new OAuth2AuthenticationException(error, ex);
        }

        if (emails == null || emails.isEmpty()) {
            return null;
        }

        Optional<String> primaryVerifiedEmail = emails.stream()
                .filter(email -> Boolean.TRUE.equals(email.primary()))
                .filter(email -> Boolean.TRUE.equals(email.verified()))
                .map(GithubEmailRecord::email)
                .filter(StringUtils::hasText)
                .findFirst();

        if (primaryVerifiedEmail.isPresent()) {
            return primaryVerifiedEmail.get();
        }

        return emails.stream()
                .filter(email -> Boolean.TRUE.equals(email.verified()))
                .map(GithubEmailRecord::email)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private record GithubEmailRecord(String email, Boolean primary, Boolean verified) {
    }
}
