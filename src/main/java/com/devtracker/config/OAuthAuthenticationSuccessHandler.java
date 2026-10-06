package com.devtracker.config;

import com.devtracker.entities.Providers;
import com.devtracker.entities.User;
import com.devtracker.repositories.UserRepository;
import com.devtracker.support.EmailNormalizer;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Robust OAuth2 Authentication Success Handler supporting Google and GitHub logins,
 * unified account linking by verified email, and profile avatar/name synchronization.
 */
public class OAuthAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public OAuthAuthenticationSuccessHandler(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        setDefaultTargetUrl("/devtracker/home");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        if (!(authentication.getPrincipal() instanceof OAuth2User oauth2User)) {
            redirectToOauthError(request, response);
            return;
        }

        // 1. Provider Identification
        String registrationId = "";
        if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
            registrationId = oauth2Token.getAuthorizedClientRegistrationId();
        }

        String rawEmail = null;
        String name = null;
        String profilePic = null;
        String providerUserId = null;
        Providers provider = Providers.LOCAL;

        // 2. Provider-Specific Attribute Extraction
        if ("google".equalsIgnoreCase(registrationId)) {
            provider = Providers.GOOGLE;
            rawEmail = extractString(oauth2User, "email");
            name = extractString(oauth2User, "name");
            profilePic = extractString(oauth2User, "picture");
            Object sub = oauth2User.getAttribute("sub");
            providerUserId = sub != null ? sub.toString() : oauth2User.getName();
        } else if ("github".equalsIgnoreCase(registrationId)) {
            provider = Providers.GITHUB;
            rawEmail = extractString(oauth2User, "email");

            // Reliable fallback if email was not populated by userInfo
            if (!StringUtils.hasText(rawEmail)) {
                String login = extractString(oauth2User, "login");
                if (StringUtils.hasText(login)) {
                    rawEmail = login + "@users.noreply.github.com";
                }
            }

            name = extractString(oauth2User, "name");
            if (!StringUtils.hasText(name)) {
                name = extractString(oauth2User, "login");
            }

            profilePic = extractString(oauth2User, "avatar_url");
            Object id = oauth2User.getAttribute("id");
            providerUserId = id != null ? id.toString() : oauth2User.getName();
        } else {
            // Generic / Fallback provider resolution
            rawEmail = extractString(oauth2User, "email");
            name = extractString(oauth2User, "name");
            profilePic = extractString(oauth2User, "picture");
            if (!StringUtils.hasText(profilePic)) {
                profilePic = extractString(oauth2User, "avatar_url");
            }
            providerUserId = oauth2User.getName();
            provider = Providers.LOCAL;
        }

        String normalizedEmail = EmailNormalizer.normalize(rawEmail);
        if (!StringUtils.hasText(normalizedEmail) || !normalizedEmail.contains("@")) {
            redirectToOauthError(request, response);
            return;
        }

        // 3. Unified Account Linking: check if an account with this email already exists
        Optional<User> existingUserOpt = userRepository.findByEmail(normalizedEmail);
        if (existingUserOpt.isEmpty()) {
            existingUserOpt = userRepository.findByEmailIgnoreCase(normalizedEmail);
        }

        if (existingUserOpt.isPresent()) {
            // User ALREADY exists (e.g. created via Form Login, Google, or GitHub)
            // DO NOT create duplicate account; link to single existing account
            User existingUser = existingUserOpt.get();

            // Sync latest profile picture from provider
            if (StringUtils.hasText(profilePic)) {
                existingUser.setProfilePic(profilePic);
            }

            // Sync name if existing name is blank or default
            if (StringUtils.hasText(name)) {
                String currentName = existingUser.getName();
                if (!StringUtils.hasText(currentName) || currentName.equalsIgnoreCase(existingUser.getEmail())) {
                    existingUser.setName(name);
                }
            }

            // Update provider & provider ID to reflect latest verified login
            existingUser.setProvider(provider);
            if (StringUtils.hasText(providerUserId)) {
                existingUser.setProviderUserId(providerUserId);
            }
            existingUser.setEmailVerified(true);

            if (existingUser.getRoleList() == null || existingUser.getRoleList().isEmpty()) {
                existingUser.setRoleList(new ArrayList<>(List.of("ROLE_USER")));
            }

            userRepository.save(existingUser);
        } else {
            // User does not exist: provision new User account
            String displayName = StringUtils.hasText(name)
                    ? name
                    : normalizedEmail.substring(0, normalizedEmail.indexOf('@'));

            User newUser = User.builder()
                    .email(normalizedEmail)
                    .name(displayName)
                    .phoneNumber("")
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .emailVerified(true)
                    .enabled(true)
                    .provider(provider)
                    .providerUserId(providerUserId)
                    .profilePic(profilePic)
                    .roleList(new ArrayList<>(List.of("ROLE_USER")))
                    .build();

            userRepository.save(newUser);
        }

        // 4. Redirect to dashboard/home page
        super.onAuthenticationSuccess(request, response, authentication);
    }

    private void redirectToOauthError(HttpServletRequest request, HttpServletResponse response) throws IOException {
        getRedirectStrategy().sendRedirect(request, response, "/login?oauthError");
    }

    private String extractString(OAuth2User oauth2User, String attributeKey) {
        Object val = oauth2User.getAttributes().get(attributeKey);
        return val instanceof String str && StringUtils.hasText(str) ? str.trim() : null;
    }
}
