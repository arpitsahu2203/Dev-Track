package com.devtracker.ai;

import java.util.List;

public record AiProblemReview(
        String keyConcept,
        String likelyStruggle,
        String recommendedApproach,
        String complexityAnalysis,
        String commonPitfalls,
        String revisionNote,
        String revisionChecklist,
        String recallQuestion,
        List<String> nextTopics,
        int revisionDays
) {
}
