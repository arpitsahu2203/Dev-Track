package com.devtracker.config;

import com.devtracker.entities.User;
import com.devtracker.services.UserService;
import com.devtracker.support.EmailNormalizer;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * Creates an OAuth-only local account on first successful OAuth sign-in.
 * A random password is stored so the account cannot be used for password login
 * until the user explicitly adds a password-reset/linking feature.
 */
public class OAuthAccountProvisioningSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public OAuthAccountProvisioningSuccessHandler(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
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
            redirectToEmailError(request, response);
            return;
        }

        String email = emailFrom(oauth2User);
        if (!StringUtils.hasText(email) || !email.contains("@") || !emailIsVerified(oauth2User)) {
            redirectToEmailError(request, response);
            return;
        }

        userService.getUserByEmail(email).orElseGet(() -> userService.saveUser(User.builder()
                .name(displayName(oauth2User, email))
                .email(email)
                .phoneNumber("OAuth account")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .emailVerified(true)
                .enabled(true)
                .build()));

        super.onAuthenticationSuccess(request, response, authentication);
    }

    private void redirectToEmailError(HttpServletRequest request, HttpServletResponse response) throws IOException {
        getRedirectStrategy().sendRedirect(request, response, "/login?oauthError");
    }

    private String emailFrom(OAuth2User oauth2User) {
        Object email = oauth2User.getAttributes().get("email");
        return email instanceof String value && StringUtils.hasText(value) ? EmailNormalizer.normalize(value) : null;
    }

    private boolean emailIsVerified(OAuth2User oauth2User) {
        Object verified = oauth2User.getAttributes().get("email_verified");
        return !(verified instanceof Boolean value && !value)
                && !(verified instanceof String text && "false".equalsIgnoreCase(text));
    }

    private String displayName(OAuth2User oauth2User, String email) {
        Map<String, Object> attributes = oauth2User.getAttributes();
        for (String key : new String[]{"name", "login"}) {
            Object value = attributes.get(key);
            if (value instanceof String text && StringUtils.hasText(text)) {
                return text.trim();
            }
        }
        return email.substring(0, email.indexOf('@'));
    }
}
