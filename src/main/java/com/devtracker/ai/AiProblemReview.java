package com.devtracker.ai;

import java.util.List;

/**
 * Structured output record for AI-generated problem reviews, designed to
 * provide actionable revision guidance without external Python dependencies.
 *
 * Uses native structured output via Spring AI and Gemini native response
 * schemas.
 */
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
                int revisionDays) {
}
