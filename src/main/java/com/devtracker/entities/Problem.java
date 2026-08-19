package com.devtracker.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "problems")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String problemName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HardnessLevel hardnessLevel;

    @Column(nullable = false)
    private String platform;

    @Column(nullable = false)
    private String problemLink;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String problemDescription;

    private Integer timeTakenToSolve;

    private LocalDate dateAdded;

    private LocalDate dateSolved;

    @Builder.Default
    private boolean revisit = false;

    @Builder.Default
    private Integer attemptsTaken = 1;

    private String topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_email", referencedColumnName = "email", nullable = false)
    private User user;

    @PrePersist
    public void setDefaultValues() {
        if (dateAdded == null) {
            dateAdded = LocalDate.now();
        }

        if (attemptsTaken == null) {
            attemptsTaken = 1;
        }
    }
}
