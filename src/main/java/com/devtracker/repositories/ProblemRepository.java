package com.devtracker.repositories;

import com.devtracker.entities.Problem;
import com.devtracker.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProblemRepository extends JpaRepository<Problem, UUID> {

    List<Problem> findByUser(User user);

    long countByUser(User user);

    long countByUserAndHardnessLevel(User user, com.devtracker.entities.HardnessLevel hardnessLevel);

    long countByUserAndDateSolvedIsNotNull(User user);

    long countByUserAndHardnessLevelAndDateSolvedIsNotNull(User user, com.devtracker.entities.HardnessLevel hardnessLevel);
}
