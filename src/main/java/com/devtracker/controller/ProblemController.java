package com.devtracker.controller;

import com.devtracker.entities.HardnessLevel;
import com.devtracker.entities.Problem;
import com.devtracker.entities.User;
import com.devtracker.form.ProblemForm;
import com.devtracker.services.ProblemService;
import com.devtracker.services.UserService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/problems")
public class ProblemController {

    private final ProblemService problemService;
    private final UserService userService;

    public ProblemController(ProblemService problemService, UserService userService) {
        this.problemService = problemService;
        this.userService = userService;
    }

    @GetMapping("/add")
    public String showAddProblemPage(Model model) {
        model.addAttribute("pageTitle", "Add Problem");
        model.addAttribute("problemForm", new ProblemForm());
        model.addAttribute("hardnessLevels", HardnessLevel.values());
        return "problems/add";
    }

    @GetMapping
    public String showProblemsPage(
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateAdded,
            @RequestParam(required = false) Boolean revisit,
            @RequestParam(required = false) Integer attemptsGreaterThan,
            Authentication authentication,
            Model model
    ) {
        Optional<User> loggedInUser = resolveLoggedInUser(authentication);
        if (loggedInUser.isEmpty()) {
            model.addAttribute("pageTitle", "My Problems");
            model.addAttribute("errorMessage", "We could not match your signed-in session to a Dev Tracker account. Please sign in with the email address used for your account.");
            model.addAttribute("hardnessLevels", HardnessLevel.values());
            model.addAttribute("problems", List.of());
            model.addAttribute("totalCount", 0);
            model.addAttribute("easyCount", 0);
            model.addAttribute("mediumCount", 0);
            model.addAttribute("hardCount", 0);
            return "problems/list";
        }

        User user = loggedInUser.get();

        List<Problem> problems = problemService.getFilteredProblems(
                user,
                difficulty,
                platform,
                topic,
                dateAdded,
                revisit,
                attemptsGreaterThan
        );

        model.addAttribute("pageTitle", "My Problems");
        model.addAttribute("problems", problems);
        model.addAttribute("hardnessLevels", HardnessLevel.values());
        model.addAttribute("difficulty", difficulty);
        model.addAttribute("platform", platform);
        model.addAttribute("topic", topic);
        model.addAttribute("dateAdded", dateAdded);
        model.addAttribute("revisit", revisit);
        model.addAttribute("attemptsGreaterThan", attemptsGreaterThan);
        model.addAttribute("totalCount", problemService.countProblemsByUser(user));
        model.addAttribute("easyCount", problemService.countProblemsByUserAndDifficulty(user, "easy"));
        model.addAttribute("mediumCount", problemService.countProblemsByUserAndDifficulty(user, "medium"));
        model.addAttribute("hardCount", problemService.countProblemsByUserAndDifficulty(user, "hard"));
        return "problems/list";
    }

    @PostMapping("/add")
    public String saveProblem(
            @Valid @ModelAttribute("problemForm") ProblemForm problemForm,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        model.addAttribute("pageTitle", "Add Problem");
        model.addAttribute("hardnessLevels", HardnessLevel.values());

        if (bindingResult.hasErrors()) {
            return "problems/add";
        }

        Optional<User> loggedInUser = resolveLoggedInUser(authentication);
        if (loggedInUser.isEmpty()) {
            model.addAttribute("errorMessage", "We could not match your signed-in session to a Dev Tracker account. Please sign in with the email address used for your account.");
            return "problems/add";
        }

        User user = loggedInUser.get();

        Problem problem = Problem.builder()
                .problemName(problemForm.getProblemName())
                .hardnessLevel(problemForm.getHardnessLevel())
                .platform(problemForm.getPlatform())
                .problemLink(problemForm.getProblemLink())
                .problemDescription(problemForm.getProblemDescription())
                .timeTakenToSolve(problemForm.getTimeTakenToSolve())
                .dateSolved(problemForm.getDateSolved())
                .revisit(problemForm.isRevisit())
                .attemptsTaken(problemForm.getAttemptsTaken() == null ? 1 : problemForm.getAttemptsTaken())
                .topic(problemForm.getTopic())
                .user(user)
                .build();

        problemService.saveProblem(problem);
        redirectAttributes.addFlashAttribute("successMessage", "Problem saved successfully.");
        return "redirect:/problems";
    }

    private Optional<User> resolveLoggedInUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        String email = resolveEmail(authentication);
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        return userService.getUserByEmail(email);
    }

    private String resolveEmail(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof OidcUser oidcUser && oidcUser.getEmail() != null) {
            return oidcUser.getEmail();
        }

        if (principal instanceof OAuth2User oauth2User) {
            Object email = oauth2User.getAttributes().get("email");
            if (email instanceof String emailValue && !emailValue.isBlank()) {
                return emailValue;
            }

            Object login = oauth2User.getAttributes().get("login");
            if (login instanceof String loginValue && !loginValue.isBlank()) {
                return loginValue;
            }
        }

        String name = authentication.getName();
        return name == null || name.isBlank() ? null : name;
    }
}
