package com.devtracker.ai;

import com.devtracker.entities.Problem;
import com.devtracker.entities.ProblemAiReview;
import com.devtracker.repositories.ProblemAiReviewRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProblemAiReviewService {

    private static final String MODEL_NAME = "qwen3:4b";
    private static final List<String> APPROVED_TAGS = List.of(
            "Array", "String", "Sorting", "Two Pointers", "Linked List", "Simulation", "Matrix", "Stack",
            "Hash Table", "Math", "Depth-First Search", "Greedy", "Tree", "Binary Tree", "Breadth-First Search",
            "Bit Manipulation", "Dynamic Programming", "Divide and Conquer", "Backtracking", "Topological Sort",
            "Data Stream", "Union Find", "Rolling Hash", "Quickselect"
    );

    private final ChatClient chatClient;
    private final ProblemAiReviewRepository reviewRepository;

    public ProblemAiReviewService(ChatClient.Builder chatClientBuilder, ProblemAiReviewRepository reviewRepository) {
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        You are a supportive DSA revision coach. Use only the supplied problem data.
                        Never claim certainty about a user's internal reasoning; use careful language such as \"You may have found...\".
                        Do not invent facts about the problem, external links, or results. Give a detailed but practical learning review.
                        Explain the recommended approach clearly enough to guide a future retry, without providing full code.
                        Include likely complexity, realistic pitfalls, and an actionable revision checklist.
                        Select nextTopics only from the provided approved tags. revisionDays must be between 1 and 30.
                        """)
                .build();
        this.reviewRepository = reviewRepository;
    }

    public ProblemAiReview generateReview(Problem problem) {
        AiProblemReview generatedReview;
        try {
            generatedReview = chatClient.prompt()
                    .user(buildPrompt(problem))
                    .call()
                    .entity(AiProblemReview.class, spec -> spec.useProviderStructuredOutput());
        } catch (RuntimeException exception) {
            throw new AiReviewGenerationException("Ollama could not create the review. Ensure Ollama is running and the "
                    + MODEL_NAME + " model has been downloaded.", exception);
        }

        if (generatedReview == null) {
            throw new AiReviewGenerationException("Ollama returned no review. Please try again.", null);
        }

        List<String> approvedNextTopics = (generatedReview.nextTopics() == null ? List.<String>of() : generatedReview.nextTopics()).stream()
                .filter(APPROVED_TAGS::contains)
                .distinct()
                .limit(3)
                .toList();
        int revisionDays = Math.clamp(generatedReview.revisionDays(), 1, 30);

        ProblemAiReview review = ProblemAiReview.builder()
                .problem(problem)
                .keyConcept(requireText(generatedReview.keyConcept(), "key concept"))
                .likelyStruggle(requireText(generatedReview.likelyStruggle(), "possible struggle"))
                .recommendedApproach(requireText(generatedReview.recommendedApproach(), "recommended approach"))
                .complexityAnalysis(requireText(generatedReview.complexityAnalysis(), "complexity analysis"))
                .commonPitfalls(requireText(generatedReview.commonPitfalls(), "common pitfalls"))
                .revisionNote(requireText(generatedReview.revisionNote(), "revision note"))
                .revisionChecklist(requireText(generatedReview.revisionChecklist(), "revision checklist"))
                .recallQuestion(requireText(generatedReview.recallQuestion(), "recall question"))
                .nextTopics(String.join(", ", approvedNextTopics))
                .suggestedRevisionDate(LocalDate.now().plusDays(revisionDays))
                .generatedAt(LocalDateTime.now())
                .modelName(MODEL_NAME)
                .build();

        reviewRepository.findByProblemIn(List.of(problem)).stream().findFirst()
                .ifPresent(existingReview -> reviewRepository.delete(existingReview));
        return reviewRepository.save(review);
    }

    public Map<UUID, ProblemAiReview> getReviewsByProblemId(Collection<Problem> problems) {
        return reviewRepository.findByProblemIn(problems).stream()
                .collect(Collectors.toMap(review -> review.getProblem().getId(), Function.identity()));
    }

    private String buildPrompt(Problem problem) {
        return """
                Create a detailed revision review for this completed DSA problem.
                For recommendedApproach, give 3 to 5 ordered conceptual steps, without code.
                For complexityAnalysis, state the expected time and space complexity and why it is appropriate.
                For commonPitfalls, list 2 to 4 concrete mistakes to avoid.
                For revisionChecklist, give 3 brief actions the user can take during revision.
                Keep revisionNote memorable and concise.

                Problem name: %s
                Difficulty: %s
                Platform: %s
                Tags: %s
                Attempts: %s
                Time taken in minutes: %s
                User description/notes: %s
                Date solved: %s
                Approved tags: %s
                """.formatted(
                clean(problem.getProblemName()),
                problem.getHardnessLevel(),
                clean(problem.getPlatform()),
                clean(problem.getTopic()),
                problem.getAttemptsTaken(),
                problem.getTimeTakenToSolve(),
                clean(problem.getProblemDescription()),
                problem.getDateSolved(),
                String.join(", ", APPROVED_TAGS)
        );
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? "Not provided" : value.trim();
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new AiReviewGenerationException("Ollama returned an incomplete " + fieldName + ". Please try again.", null);
        }
        return value.trim();
    }

    public static class AiReviewGenerationException extends RuntimeException {
        public AiReviewGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
