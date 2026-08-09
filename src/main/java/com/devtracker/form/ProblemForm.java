package com.devtracker.form;

import com.devtracker.entities.HardnessLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ProblemForm {

    @NotBlank(message = "Problem name is required")
    private String problemName;

    @NotNull(message = "Hardness level is required")
    private HardnessLevel hardnessLevel;

    @NotBlank(message = "Platform is required")
    private String platform;

    @NotBlank(message = "Problem link is required")
    private String problemLink;

    @NotBlank(message = "Problem description is required")
    private String problemDescription;

    private Integer timeTakenToSolve;

    private LocalDate dateSolved;

    private boolean revisit;

    private Integer attemptsTaken = 1;

    private String topic;
}
