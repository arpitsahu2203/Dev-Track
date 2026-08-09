package com.devtracker.controller;

import com.devtracker.entities.HardnessLevel;
import com.devtracker.entities.Problem;
import com.devtracker.entities.User;
import com.devtracker.form.ProblemForm;
import com.devtracker.services.ProblemService;
import com.devtracker.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.ui.ConcurrentModel;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProblemControllerTest {

    @Test
    void saveProblemUsesOauth2EmailAttributeWhenResolvingUser() {
        User storedUser = User.builder()
                .email("dev@example.com")
                .name("Dev")
                .phoneNumber("1234567890")
                .password("secret")
                .enabled(true)
                .emailVerified(true)
                .build();

        CapturingProblemService problemService = new CapturingProblemService();
        ProblemController controller = new ProblemController(problemService, new StubUserService(storedUser));

        ProblemForm form = new ProblemForm();
        form.setProblemName("Two Sum");
        form.setHardnessLevel(HardnessLevel.EASY);
        form.setPlatform("LeetCode");
        form.setProblemLink("https://example.com/two-sum");
        form.setProblemDescription("Classic array problem");
        form.setTimeTakenToSolve(12);
        form.setDateSolved(LocalDate.of(2026, 7, 13));
        form.setRevisit(true);
        form.setAttemptsTaken(2);
        form.setTopic("Arrays");

        BindingResult bindingResult = new BeanPropertyBindingResult(form, "problemForm");
        ModelAndRedirectAttributes modelAndRedirectAttributes = new ModelAndRedirectAttributes();
        Authentication authentication = oauthAuthentication("dev@example.com");

        String viewName = controller.saveProblem(form, bindingResult, authentication, modelAndRedirectAttributes.model, modelAndRedirectAttributes.redirectAttributes);

        assertEquals("redirect:/problems", viewName);
        assertEquals("Two Sum", problemService.savedProblem.getProblemName());
        assertSame(storedUser, problemService.savedProblem.getUser());
    }

    @Test
    void saveProblemReturnsAddPageWhenNoLocalAccountExists() {
        ProblemController controller = new ProblemController(new CapturingProblemService(), new EmptyUserService());

        ProblemForm form = new ProblemForm();
        form.setProblemName("Two Sum");
        form.setHardnessLevel(HardnessLevel.EASY);
        form.setPlatform("LeetCode");
        form.setProblemLink("https://example.com/two-sum");
        form.setProblemDescription("Classic array problem");

        BindingResult bindingResult = new BeanPropertyBindingResult(form, "problemForm");
        ConcurrentModel model = new ConcurrentModel();

        String viewName = controller.saveProblem(form, bindingResult, oauthAuthentication("missing@example.com"), model, new RedirectAttributesModelMap());

        assertEquals("problems/add", viewName);
        assertTrue(model.containsAttribute("errorMessage"));
    }

    private static Authentication oauthAuthentication(String email) {
        OAuth2User principal = new OAuth2User() {
            @Override
            public Map<String, Object> getAttributes() {
                return Map.of("email", email, "login", email);
            }

            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return List.of();
            }

            @Override
            public String getName() {
                return email;
            }
        };

        return new UsernamePasswordAuthenticationToken(principal, "n/a", principal.getAuthorities());
    }

    private static final class CapturingProblemService implements ProblemService {
        private Problem savedProblem;

        @Override
        public Problem saveProblem(Problem problem) {
            this.savedProblem = problem;
            return problem;
        }

        @Override
        public Optional<Problem> getProblemById(java.util.UUID id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Problem> getAllProblems() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Problem> getProblemsByUser(User user) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Problem> getFilteredProblems(User user, String difficulty, String platform, String topic, LocalDate dateAdded, Boolean revisit, Integer attemptsGreaterThan) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long countProblemsByUser(User user) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long countProblemsByUserAndDifficulty(User user, String difficulty) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Problem updateProblem(java.util.UUID id, Problem problem) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteProblem(java.util.UUID id) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class StubUserService implements UserService {
        private final User storedUser;

        private StubUserService(User storedUser) {
            this.storedUser = storedUser;
        }

        @Override
        public User saveUser(User user) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<User> getUserByEmail(String email) {
            return storedUser.getEmail().equals(email) ? Optional.of(storedUser) : Optional.empty();
        }

        @Override
        public List<User> getAllUsers() {
            throw new UnsupportedOperationException();
        }

        @Override
        public User updateUser(String email, User user) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteUser(String email) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class EmptyUserService implements UserService {
        @Override
        public User saveUser(User user) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<User> getUserByEmail(String email) {
            return Optional.empty();
        }

        @Override
        public List<User> getAllUsers() {
            throw new UnsupportedOperationException();
        }

        @Override
        public User updateUser(String email, User user) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteUser(String email) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class ModelAndRedirectAttributes {
        private final ConcurrentModel model = new ConcurrentModel();
        private final RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
    }
}