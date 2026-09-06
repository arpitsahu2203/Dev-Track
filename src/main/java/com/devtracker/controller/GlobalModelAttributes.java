package com.devtracker.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

@ControllerAdvice
public class GlobalModelAttributes {

    /**
     * Resolves Spring Security's deferred CSRF token before Thymeleaf starts
     * streaming a page. Otherwise a large page can commit its response before
     * the first form action attempts to create the HTTP session.
     */
    @ModelAttribute
    public void initializeCsrfToken(HttpServletRequest request) {
        Object tokenAttribute = request.getAttribute(CsrfToken.class.getName());
        if (tokenAttribute instanceof CsrfToken csrfToken) {
            csrfToken.getToken();
        }
    }

    @ModelAttribute("authenticated")
    public boolean authenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
