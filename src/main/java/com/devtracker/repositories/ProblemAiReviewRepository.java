package com.devtracker.repositories;

import com.devtracker.entities.Problem;
import com.devtracker.entities.ProblemAiReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ProblemAiReviewRepository extends JpaRepository<ProblemAiReview, UUID> {

    List<ProblemAiReview> findByProblemIn(Collection<Problem> problems);
}
