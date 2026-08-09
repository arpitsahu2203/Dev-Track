package com.devtracker.services;

import com.devtracker.entities.Problem;
import com.devtracker.entities.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProblemService {

    Problem saveProblem(Problem problem);

    Optional<Problem> getProblemById(UUID id);

    List<Problem> getAllProblems();

    List<Problem> getProblemsByUser(User user);

    List<Problem> getFilteredProblems(
            User user,
            String difficulty,
            String platform,
            String topic,
            LocalDate dateAdded,
            Boolean revisit,
            Integer attemptsGreaterThan
    );

    long countProblemsByUser(User user);

    long countProblemsByUserAndDifficulty(User user, String difficulty);

    Problem updateProblem(UUID id, Problem problem);

    void deleteProblem(UUID id);
}
