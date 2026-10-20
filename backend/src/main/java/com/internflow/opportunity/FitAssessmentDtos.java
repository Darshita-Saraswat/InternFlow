package com.internflow.opportunity;

import java.util.List;
import java.util.UUID;

public final class FitAssessmentDtos {

	private FitAssessmentDtos() {
	}

	public enum MatchStatus {
		MATCH,
		PARTIAL_MATCH,
		NO_MATCH,
		UNKNOWN
	}

	public enum GraduationEligibility {
		ELIGIBLE,
		INELIGIBLE,
		UNKNOWN
	}

	public record MatchResult(MatchStatus status, String explanation) {
	}

	public record FitAssessmentResponse(
			UUID opportunityId,
			MatchStatus skillsStatus,
			List<String> skillsMatched,
			List<String> requiredSkillsNotListed,
			Integer totalRequiredSkills,
			Integer skillsMatchCount,
			Integer skillsMatchPercentage,
			String skillsExplanation,
			GraduationEligibility graduationEligibility,
			String graduationExplanation,
			MatchResult roleAlignment,
			MatchResult locationPreferenceMatch,
			MatchResult workModePreferenceMatch,
			List<String> explanations,
			List<String> suggestedNextSteps) {
	}
}
