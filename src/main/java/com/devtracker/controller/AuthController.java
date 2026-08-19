package com.devtracker.controller;

import com.devtracker.entities.User;
import com.devtracker.form.LoginForm;
import com.devtracker.form.UserForm;
import com.devtracker.services.UserService;
import com.devtracker.support.EmailNormalizer;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("pageTitle", "Register");
        model.addAttribute("userForm", new UserForm());
        return "user/register";
    }

    @PostMapping("/register")
    public String registerUser(
            @Valid @ModelAttribute("userForm") UserForm userForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        model.addAttribute("pageTitle", "Register");
        String normalizedEmail = EmailNormalizer.normalize(userForm.getEmail());
        userForm.setEmail(normalizedEmail);

        if (!bindingResult.hasFieldErrors("email") && normalizedEmail != null
                && userService.getUserByEmail(normalizedEmail).isPresent()) {
            bindingResult.rejectValue("email", "email.exists", "An account with this email already exists");
        }

        if (bindingResult.hasErrors()) {
            return "user/register";
        }

        User user = User.builder()
                .name(userForm.getName())
                .email(userForm.getEmail())
                .phoneNumber(userForm.getPhoneNumber())
                .password(passwordEncoder.encode(userForm.getPassword()))
                .emailVerified(false)
                .enabled(true)
                .build();

        userService.saveUser(user);
        redirectAttributes.addFlashAttribute("successMessage", "Registration successful. Please login.");
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String showLoginPage(Model model) {
        model.addAttribute("pageTitle", "Login");
        model.addAttribute("loginForm", new LoginForm());
        return "user/login";
    }

}
