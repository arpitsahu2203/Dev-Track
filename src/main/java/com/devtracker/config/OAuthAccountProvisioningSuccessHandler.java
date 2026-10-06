package com.devtracker.config;

import com.devtracker.repositories.UserRepository;
import com.devtracker.services.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Backward-compatible extension of OAuthAuthenticationSuccessHandler.
 */
public class OAuthAccountProvisioningSuccessHandler extends OAuthAuthenticationSuccessHandler {

    public OAuthAccountProvisioningSuccessHandler(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        super(userRepository, passwordEncoder);
    }

    public OAuthAccountProvisioningSuccessHandler(UserService userService, PasswordEncoder passwordEncoder, UserRepository userRepository) {
        super(userRepository, passwordEncoder);
    }
}
