package com.devtracker.controller;

import com.devtracker.ai.ProblemAiReviewService;
import com.devtracker.entities.HardnessLevel;
import com.devtracker.entities.Problem;
import com.devtracker.entities.User;
import com.devtracker.form.ProblemForm;
import com.devtracker.services.ProblemService;
import com.devtracker.services.UserService;
import com.devtracker.support.EmailNormalizer;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/problems")
public class ProblemController {

    private static final List<String> PLATFORMS = List.of(
            "LeetCode", "GeeksforGeeks", "Codeforces", "CodeChef", "HackerRank",
            "HackerEarth", "AtCoder", "CSES", "InterviewBit", "Coding Ninjas", "Other"
    );

    private static final Map<String, List<String>> TAG_GROUPS = createTagGroups();

    private final ProblemService problemService;
    private final UserService userService;
    private final ProblemAiReviewService aiReviewService;

    @Autowired
    public ProblemController(ProblemService problemService, UserService userService, ProblemAiReviewService aiReviewService) {
        this.problemService = problemService;
        this.userService = userService;
        this.aiReviewService = aiReviewService;
    }

    public ProblemController(ProblemService problemService, UserService userService) {
        this(problemService, userService, null);
    }

    @GetMapping("/add")
    public String showAddProblemPage(Model model) {
        model.addAttribute("pageTitle", "Add Problem");
        model.addAttribute("problemForm", new ProblemForm());
        populateFormOptions(model);
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
            model.addAttribute("errorMessage", accountResolutionMessage(authentication));
            model.addAttribute("hardnessLevels", HardnessLevel.values());
            model.addAttribute("platforms", PLATFORMS);
            model.addAttribute("topicTags", TAG_GROUPS.values().stream().flatMap(List::stream).toList());
            model.addAttribute("problems", List.of());
            model.addAttribute("totalCount", 0);
            model.addAttribute("easyCount", 0);
            model.addAttribute("mediumCount", 0);
            model.addAttribute("hardCount", 0);
            model.addAttribute("topicStats", Map.of());
            model.addAttribute("maxTopicCount", 0);
            return "problems/list";
        }

        User user = loggedInUser.get();
        List<Problem> allProblems = problemService.getProblemsByUser(user);

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
        model.addAttribute("aiReviewsByProblemId", aiReviewService.getReviewsByProblemId(problems));
        model.addAttribute("hardnessLevels", HardnessLevel.values());
        model.addAttribute("platforms", PLATFORMS);
        model.addAttribute("topicTags", TAG_GROUPS.values().stream().flatMap(List::stream).toList());
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
        Map<String, Long> topicStats = getSolvedTopicStats(allProblems);
        model.addAttribute("topicStats", topicStats);
        model.addAttribute("maxTopicCount", topicStats.values().stream().mapToLong(Long::longValue).max().orElse(0));
        return "problems/list";
    }

    @PostMapping("/{id}/ai-review")
    public String generateAiReview(
            @PathVariable java.util.UUID id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        Optional<User> loggedInUser = resolveLoggedInUser(authentication);
        Optional<Problem> problem = problemService.getProblemById(id);

        if (loggedInUser.isEmpty() || problem.isEmpty()
                || !problem.get().getUser().getEmail().equalsIgnoreCase(loggedInUser.get().getEmail())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Problem not found.");
            return "redirect:/problems";
        }

        try {
            aiReviewService.generateReview(problem.get());
            redirectAttributes.addFlashAttribute("successMessage", "AI review generated. Use it to guide your next revision.");
        } catch (ProblemAiReviewService.AiReviewGenerationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/problems";
    }

    @PostMapping("/{id}/delete")
    public String deleteProblem(
            @PathVariable java.util.UUID id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        Optional<User> loggedInUser = resolveLoggedInUser(authentication);
        Optional<Problem> problem = problemService.getProblemById(id);

        if (loggedInUser.isEmpty() || problem.isEmpty()
                || !problem.get().getUser().getEmail().equalsIgnoreCase(loggedInUser.get().getEmail())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Problem not found.");
            return "redirect:/problems";
        }

        problemService.deleteProblem(id);
        redirectAttributes.addFlashAttribute("successMessage", "Problem deleted successfully.");
        return "redirect:/problems";
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
        populateFormOptions(model);

        if (bindingResult.hasErrors()) {
            return "problems/add";
        }

        Optional<User> loggedInUser = resolveLoggedInUser(authentication);
        if (loggedInUser.isEmpty()) {
            model.addAttribute("errorMessage", accountResolutionMessage(authentication));
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
                .topic(problemForm.getTopics().isEmpty()
                        ? problemForm.getTopic()
                        : String.join(", ", problemForm.getTopics()))
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
            return EmailNormalizer.normalize(oidcUser.getEmail());
        }

        if (principal instanceof OAuth2User oauth2User) {
            Object email = oauth2User.getAttributes().get("email");
            if (email instanceof String emailValue && !emailValue.isBlank()) {
                return EmailNormalizer.normalize(emailValue);
            }
        }

        String name = authentication.getName();
        if (name == null || name.isBlank() || !name.contains("@")) {
            return null;
        }
        return EmailNormalizer.normalize(name);
    }

    private void populateFormOptions(Model model) {
        model.addAttribute("hardnessLevels", HardnessLevel.values());
        model.addAttribute("platforms", PLATFORMS);
        model.addAttribute("tagGroups", TAG_GROUPS);
    }

    private Map<String, Long> getSolvedTopicStats(List<Problem> problems) {
        return problems.stream()
                .filter(problem -> problem.getDateSolved() != null)
                .map(Problem::getTopic)
                .filter(topic -> topic != null && !topic.isBlank())
                .flatMap(topic -> Arrays.stream(topic.split("\\s*,\\s*")))
                .map(String::trim)
                .filter(topic -> !topic.isBlank())
                .collect(Collectors.groupingBy(topic -> topic, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER)))
                .limit(8)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private static Map<String, List<String>> createTagGroups() {
        Map<String, List<String>> tagGroups = new LinkedHashMap<>();
        tagGroups.put("Fundamental", List.of(
                "Array", "String", "Sorting", "Two Pointers", "Linked List", "Simulation", "Matrix", "Stack"
        ));
        tagGroups.put("Intermediate", List.of(
                "Hash Table", "Math", "Greedy", "Tree", "Binary Tree", "Bit Manipulation"
        ));
        tagGroups.put("Graph", List.of(
                "Graph", "Directed Graph", "Undirected Graph", "Weighted Graph", "Graph Traversal",
                "Breadth-First Search", "Depth-First Search", "Topological Sort", "Shortest Path",
                "Dijkstra's Algorithm", "Bellman-Ford", "Floyd-Warshall", "Minimum Spanning Tree",
                "Kruskal's Algorithm", "Prim's Algorithm", "Union Find", "Strongly Connected Components",
                "Bridges and Articulation Points", "Eulerian Path", "Hamiltonian Path", "Network Flow"
        ));
        tagGroups.put("Advanced", List.of(
                "Dynamic Programming", "Divide and Conquer", "Backtracking", "Data Stream", "Rolling Hash", "Quickselect"
        ));
        return Collections.unmodifiableMap(tagGroups);
    }

    private String accountResolutionMessage(Authentication authentication) {
        if (resolveEmail(authentication) == null) {
            if (!(authentication instanceof OAuth2AuthenticationToken token)
                    || !"github".equalsIgnoreCase(token.getAuthorizedClientRegistrationId())) {
                return "We could not obtain an email from your sign-in provider. Please sign in with the email address registered with Dev Tracker.";
            }
            return "We could not obtain a verified email from your GitHub account. Please add a primary verified email in GitHub, then sign in with the email address registered with Dev Tracker.";
        }
        return "We could not match your signed-in session to a Dev Tracker account. Please sign in with the email address used for your account.";
    }
}
