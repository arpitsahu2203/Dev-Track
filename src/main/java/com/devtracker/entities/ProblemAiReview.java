package com.devtracker.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "problem_ai_reviews")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProblemAiReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false, unique = true)
    private Problem problem;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String keyConcept;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String likelyStruggle;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String recommendedApproach;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String complexityAnalysis;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String commonPitfalls;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String revisionNote;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String revisionChecklist;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String recallQuestion;

    @Column(nullable = false)
    private String nextTopics;

    @Column(nullable = false)
    private LocalDate suggestedRevisionDate;

    @Column(nullable = false)
    private LocalDateTime generatedAt;

    @Column(nullable = false)
    private String modelName;
}
