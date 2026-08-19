package com.devtracker.serviceImplementation;

import com.devtracker.entities.Problem;
import com.devtracker.entities.User;
import com.devtracker.repositories.ProblemRepository;
import com.devtracker.services.ProblemService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Arrays;

@Service
public class ProblemServiceImplementation implements ProblemService {

    private final ProblemRepository problemRepository;

    public ProblemServiceImplementation(ProblemRepository problemRepository) {
        this.problemRepository = problemRepository;
    }

    @Override
    public Problem saveProblem(Problem problem) {
        return problemRepository.save(problem);
    }

    @Override
    public Optional<Problem> getProblemById(UUID id) {
        return problemRepository.findById(id);
    }

    @Override
    public List<Problem> getAllProblems() {
        return problemRepository.findAll();
    }

    @Override
    public List<Problem> getProblemsByUser(User user) {
        return problemRepository.findByUser(user);
    }

    @Override
    public List<Problem> getFilteredProblems(
            User user,
            String difficulty,
            String platform,
            String topic,
            LocalDate dateAdded,
            Boolean revisit,
            Integer attemptsGreaterThan
    ) {
        return problemRepository.findByUser(user).stream()
                .filter(problem -> difficulty == null || difficulty.isBlank()
                        || problem.getHardnessLevel().name().equalsIgnoreCase(difficulty))
                .filter(problem -> platform == null || platform.isBlank()
                        || problem.getPlatform().equalsIgnoreCase(platform))
                .filter(problem -> topic == null || topic.isBlank()
                        || (problem.getTopic() != null && Arrays.stream(problem.getTopic().split("\\s*,\\s*"))
                        .anyMatch(savedTopic -> savedTopic.equalsIgnoreCase(topic))))
                .filter(problem -> dateAdded == null || dateAdded.equals(problem.getDateAdded()))
                .filter(problem -> revisit == null || problem.isRevisit() == revisit)
                .filter(problem -> attemptsGreaterThan == null
                        || (problem.getAttemptsTaken() != null && problem.getAttemptsTaken() > attemptsGreaterThan))
                .toList();
    }

    @Override
    public long countProblemsByUser(User user) {
        return problemRepository.countByUserAndDateSolvedIsNotNull(user);
    }

    @Override
    public long countProblemsByUserAndDifficulty(User user, String difficulty) {
        return problemRepository.countByUserAndHardnessLevelAndDateSolvedIsNotNull(
                user,
                com.devtracker.entities.HardnessLevel.valueOf(difficulty.toUpperCase())
        );
    }

    @Override
    public Problem updateProblem(UUID id, Problem problem) {
        Problem existingProblem = problemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Problem not found with id: " + id));

        existingProblem.setProblemName(problem.getProblemName());
        existingProblem.setHardnessLevel(problem.getHardnessLevel());
        existingProblem.setPlatform(problem.getPlatform());
        existingProblem.setProblemLink(problem.getProblemLink());
        existingProblem.setProblemDescription(problem.getProblemDescription());
        existingProblem.setTimeTakenToSolve(problem.getTimeTakenToSolve());
        existingProblem.setDateAdded(problem.getDateAdded());
        existingProblem.setDateSolved(problem.getDateSolved());
        existingProblem.setRevisit(problem.isRevisit());
        existingProblem.setAttemptsTaken(problem.getAttemptsTaken());
        existingProblem.setTopic(problem.getTopic());
        existingProblem.setUser(problem.getUser());

        return problemRepository.save(existingProblem);
    }

    @Override
    public void deleteProblem(UUID id) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Problem not found with id: " + id));
        problemRepository.delete(problem);
    }
}
