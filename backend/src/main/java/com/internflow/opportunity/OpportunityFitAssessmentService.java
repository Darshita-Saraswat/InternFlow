package com.internflow.opportunity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.internflow.opportunity.FitAssessmentDtos.FitAssessmentResponse;
import com.internflow.opportunity.FitAssessmentDtos.GraduationEligibility;
import com.internflow.opportunity.FitAssessmentDtos.MatchResult;
import com.internflow.opportunity.FitAssessmentDtos.MatchStatus;
import com.internflow.opportunity.OpportunityDtos.OpportunityResponse;
import com.internflow.profile.StudentProfileDtos.ProfileResponse;
import com.internflow.profile.StudentProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpportunityFitAssessmentService {

	private final OpportunityService opportunityService;
	private final StudentProfileService studentProfileService;

	public OpportunityFitAssessmentService(
			OpportunityService opportunityService,
			StudentProfileService studentProfileService) {
		this.opportunityService = opportunityService;
		this.studentProfileService = studentProfileService;
	}

	@Transactional(readOnly = true)
	public FitAssessmentResponse assess(UUID studentId, UUID opportunityId) {
		OpportunityResponse opportunity = opportunityService.get(opportunityId);
		ProfileResponse profile = studentProfileService.get(studentId);

		SkillAssessment skills = assessSkills(opportunity.requiredSkills(), profile.skills());
		GraduationAssessment graduation = assessGraduation(opportunity, profile.graduationDate());
		MatchResult role = assessRole(opportunity, profile.targetRoleCategories());
		MatchResult location = assessLocation(opportunity.location(), profile.preferredLocations());
		MatchResult workMode = assessWorkMode(opportunity.workMode(), profile.workModes());

		List<String> explanations = List.of(
				skills.explanation(),
				graduation.explanation(),
				role.explanation(),
				location.explanation(),
				workMode.explanation());
		List<String> nextSteps = suggestedNextSteps(
				skills, graduation, role, location, workMode);

		return new FitAssessmentResponse(
				opportunity.id(),
				skills.status(),
				skills.matched(),
				skills.notListed(),
				skills.totalRequired(),
				skills.matchCount(),
				skills.matchPercentage(),
				skills.explanation(),
				graduation.eligibility(),
				graduation.explanation(),
				role,
				location,
				workMode,
				explanations,
				nextSteps);
	}

	private static SkillAssessment assessSkills(List<String> requiredSkills, List<String> profileSkills) {
		Map<String, String> distinctRequired = distinctByNormalizedValue(requiredSkills);
		int totalRequired = distinctRequired.size();
		if (totalRequired == 0) {
			return new SkillAssessment(
					MatchStatus.UNKNOWN, List.of(), List.of(), 0, null, null,
					"No required skills are listed for this catalogue opportunity.");
		}
		if (profileSkills.isEmpty()) {
			return new SkillAssessment(
					MatchStatus.UNKNOWN, List.of(), List.of(), totalRequired, null, null,
					"Your profile does not list skills, so skills alignment cannot be assessed.");
		}

		Map<String, String> normalizedProfileSkills = distinctByNormalizedValue(profileSkills);
		List<String> matched = new ArrayList<>();
		List<String> notListed = new ArrayList<>();
		for (Map.Entry<String, String> required : distinctRequired.entrySet()) {
			if (normalizedProfileSkills.containsKey(required.getKey())) {
				matched.add(required.getValue());
			} else {
				notListed.add(required.getValue());
			}
		}
		int count = matched.size();
		MatchStatus status = count == totalRequired
				? MatchStatus.MATCH
				: count == 0 ? MatchStatus.NO_MATCH : MatchStatus.PARTIAL_MATCH;
		int percentage = count * 100 / totalRequired;
		return new SkillAssessment(
				status,
				List.copyOf(matched),
				List.copyOf(notListed),
				totalRequired,
				count,
				percentage,
				"Skills are matched by case-insensitive exact text after trimming whitespace; no semantic equivalence is inferred. "
						+ count + " of " + totalRequired + " distinct required skills are listed in your profile.");
	}

	private static GraduationAssessment assessGraduation(
			OpportunityResponse opportunity,
			LocalDate graduationDate) {
		Integer from = opportunity.eligibleGraduationFrom();
		Integer to = opportunity.eligibleGraduationTo();
		if (from == null || to == null || graduationDate == null) {
			return new GraduationAssessment(
					GraduationEligibility.UNKNOWN,
					"Graduation eligibility is unknown because the profile graduation date and both opportunity graduation-year bounds are required.");
		}
		int graduationYear = graduationDate.getYear();
		boolean eligible = graduationYear >= from && graduationYear <= to;
		return new GraduationAssessment(
				eligible ? GraduationEligibility.ELIGIBLE : GraduationEligibility.INELIGIBLE,
				"Your profile graduation year (" + graduationYear + ") "
						+ (eligible ? "falls within" : "falls outside")
						+ " the eligible range " + from + "–" + to + " (inclusive).");
	}

	private static MatchResult assessRole(OpportunityResponse opportunity, List<String> targetRoles) {
		if (targetRoles.isEmpty()) {
			return new MatchResult(MatchStatus.UNKNOWN, "No target role categories are listed in your profile.");
		}
		String roleType = normalize(opportunity.roleType());
		String roleTitle = normalize(opportunity.roleTitle());
		boolean matches = targetRoles.stream()
				.map(OpportunityFitAssessmentService::normalize)
				.anyMatch(target -> target.equals(roleType) || target.equals(roleTitle));
		return new MatchResult(
				matches ? MatchStatus.MATCH : MatchStatus.NO_MATCH,
				matches
						? "A profile target role exactly matches the opportunity role type or title after trimming and case normalization."
						: "No profile target role exactly matches the opportunity role type or title after trimming and case normalization.");
	}

	private static MatchResult assessLocation(String opportunityLocation, List<String> preferredLocations) {
		if (preferredLocations.isEmpty()) {
			return new MatchResult(MatchStatus.UNKNOWN, "No preferred locations are listed in your profile.");
		}
		if (opportunityLocation == null || opportunityLocation.isBlank()) {
			return new MatchResult(MatchStatus.UNKNOWN, "The opportunity does not specify a location.");
		}
		boolean matches = preferredLocations.stream()
				.map(OpportunityFitAssessmentService::normalize)
				.anyMatch(normalize(opportunityLocation)::equals);
		return new MatchResult(
				matches ? MatchStatus.MATCH : MatchStatus.NO_MATCH,
				matches
						? "The opportunity location exactly matches a preferred location after trimming and case normalization."
						: "The opportunity location does not exactly match any preferred location after trimming and case normalization.");
	}

	private static MatchResult assessWorkMode(String opportunityWorkMode, List<String> preferredWorkModes) {
		if (preferredWorkModes.isEmpty()) {
			return new MatchResult(MatchStatus.UNKNOWN, "No preferred work modes are listed in your profile.");
		}
		if (opportunityWorkMode == null || opportunityWorkMode.isBlank()) {
			return new MatchResult(MatchStatus.UNKNOWN, "The opportunity does not specify a work mode.");
		}
		boolean matches = preferredWorkModes.stream()
				.map(OpportunityFitAssessmentService::normalize)
				.anyMatch(normalize(opportunityWorkMode)::equals);
		return new MatchResult(
				matches ? MatchStatus.MATCH : MatchStatus.NO_MATCH,
				matches
						? "The opportunity work mode matches a preferred work mode."
						: "The opportunity work mode does not match any preferred work mode.");
	}

	private static List<String> suggestedNextSteps(
			SkillAssessment skills,
			GraduationAssessment graduation,
			MatchResult role,
			MatchResult location,
			MatchResult workMode) {
		List<String> steps = new ArrayList<>();
		if (skills.status() == MatchStatus.UNKNOWN) {
			steps.add(skills.totalRequired() == 0
					? "Review the opportunity details or employer listing for skill requirements."
					: "Add your skills to your student profile to assess skills alignment.");
		} else if (!skills.notListed().isEmpty()) {
			steps.add("If accurate, add relevant experience with these skills to your profile: "
					+ String.join(", ", skills.notListed()) + ".");
		}
		if (graduation.eligibility() == GraduationEligibility.UNKNOWN) {
			steps.add("Check that your graduation date and the opportunity's full eligible graduation-year range are available.");
		} else if (graduation.eligibility() == GraduationEligibility.INELIGIBLE) {
			steps.add("Review the employer's eligibility requirements before applying.");
		}
		if (role.status() == MatchStatus.UNKNOWN) {
			steps.add("Add target role categories to your profile for a role-alignment comparison.");
		}
		if (location.status() == MatchStatus.UNKNOWN) {
			steps.add("Add preferred locations to your profile or check the opportunity listing for its location.");
		}
		if (workMode.status() == MatchStatus.UNKNOWN) {
			steps.add("Add preferred work modes to your profile or check the opportunity listing for its work mode.");
		}
		if (steps.isEmpty()) {
			steps.add("Review the opportunity details and confirm current requirements with the listed source.");
		}
		return List.copyOf(steps);
	}

	private static Map<String, String> distinctByNormalizedValue(List<String> values) {
		Map<String, String> distinct = new LinkedHashMap<>();
		for (String value : values) {
			distinct.putIfAbsent(normalize(value), value.trim());
		}
		return distinct;
	}

	private static String normalize(String value) {
		return value.trim().toLowerCase(Locale.ROOT);
	}

	private record SkillAssessment(
			MatchStatus status,
			List<String> matched,
			List<String> notListed,
			Integer totalRequired,
			Integer matchCount,
			Integer matchPercentage,
			String explanation) {
	}

	private record GraduationAssessment(GraduationEligibility eligibility, String explanation) {
	}
}
