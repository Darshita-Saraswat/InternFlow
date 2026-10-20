package com.internflow;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.PreparedStatement;
import java.sql.Array;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.internflow.auth.AccountPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OpportunityFitAssessmentIntegrationTests {

	private static final String OPPORTUNITIES_PATH = "/api/v1/opportunities";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private final UUID studentId = UUID.randomUUID();
	private final AccountPrincipal principal = new AccountPrincipal(
			studentId, "fit-student@example.test", "Fit Student", "test-password-hash");
	private UUID opportunityId;

	@BeforeEach
	void insertTransactionalFixtures() {
		jdbcTemplate.update(
				"INSERT INTO users (id, email, display_name, password_hash) VALUES (?, ?, ?, ?)",
				studentId, "fit-student@example.test", "Fit Student", "test-password-hash");
		jdbcTemplate.update("INSERT INTO student_profiles (user_id) VALUES (?)", studentId);
		opportunityId = insertOpportunity(List.of("Java", "SQL"), 2027, 2028);
	}

	@Test
	void fullSkillMatchUsesTrimmedCaseInsensitiveExactComparison() throws Exception {
		updateProfile(
				LocalDate.of(2027, 6, 1),
				List.of("java ", "SQL"),
				List.of("Software Engineer Intern"),
				List.of(" boston "),
				List.of("remote"));

		mockMvc.perform(get(OPPORTUNITIES_PATH + "/" + opportunityId + "/fit-assessment").with(user(principal)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.skillsStatus").value("MATCH"))
				.andExpect(jsonPath("$.skillsMatched[0]").value("Java"))
				.andExpect(jsonPath("$.skillsMatched[1]").value("SQL"))
				.andExpect(jsonPath("$.requiredSkillsNotListed").isEmpty())
				.andExpect(jsonPath("$.totalRequiredSkills").value(2))
				.andExpect(jsonPath("$.skillsMatchCount").value(2))
				.andExpect(jsonPath("$.skillsMatchPercentage").value(100))
				.andExpect(jsonPath("$.graduationEligibility").value("ELIGIBLE"))
				.andExpect(jsonPath("$.roleAlignment.status").value("MATCH"))
				.andExpect(jsonPath("$.locationPreferenceMatch.status").value("MATCH"))
				.andExpect(jsonPath("$.workModePreferenceMatch.status").value("MATCH"))
				.andExpect(jsonPath("$.explanations.length()").value(5))
				.andExpect(jsonPath("$.suggestedNextSteps").isNotEmpty());
	}

	@Test
	void partialSkillMatchListsRequiredSkillsNotInProfile() throws Exception {
		updateProfile(null, List.of("Java"), List.of(), List.of(), List.of());

		mockMvc.perform(get(OPPORTUNITIES_PATH + "/" + opportunityId + "/fit-assessment").with(user(principal)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.skillsStatus").value("PARTIAL_MATCH"))
				.andExpect(jsonPath("$.skillsMatched[0]").value("Java"))
				.andExpect(jsonPath("$.requiredSkillsNotListed[0]").value("SQL"))
				.andExpect(jsonPath("$.skillsMatchCount").value(1))
				.andExpect(jsonPath("$.skillsMatchPercentage").value(50));
	}

	@Test
	void noSkillMatchReturnsZeroWithoutSemanticEquivalence() throws Exception {
		updateProfile(null, List.of("JavaScript"), List.of(), List.of(), List.of());

		mockMvc.perform(get(OPPORTUNITIES_PATH + "/" + opportunityId + "/fit-assessment").with(user(principal)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.skillsStatus").value("NO_MATCH"))
				.andExpect(jsonPath("$.skillsMatched").isEmpty())
				.andExpect(jsonPath("$.requiredSkillsNotListed.length()").value(2))
				.andExpect(jsonPath("$.skillsMatchCount").value(0))
				.andExpect(jsonPath("$.skillsMatchPercentage").value(0))
				.andExpect(jsonPath("$.skillsExplanation").value(
						org.hamcrest.Matchers.containsString("no semantic equivalence is inferred")));
	}

	@Test
	void emptyRequiredSkillsAreUnknownRatherThanPerfectMatch() throws Exception {
		setRequiredSkills(List.of());
		updateProfile(null, List.of("Java", "SQL"), List.of(), List.of(), List.of());

		mockMvc.perform(get(OPPORTUNITIES_PATH + "/" + opportunityId + "/fit-assessment").with(user(principal)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.skillsStatus").value("UNKNOWN"))
				.andExpect(jsonPath("$.skillsMatched").isEmpty())
				.andExpect(jsonPath("$.requiredSkillsNotListed").isEmpty())
				.andExpect(jsonPath("$.totalRequiredSkills").value(0))
				.andExpect(jsonPath("$.skillsMatchCount").doesNotExist())
				.andExpect(jsonPath("$.skillsMatchPercentage").doesNotExist());
	}

	@Test
	void missingProfileFieldsProduceUnknownResultsInsteadOfNegativeMatches() throws Exception {
		mockMvc.perform(get(OPPORTUNITIES_PATH + "/" + opportunityId + "/fit-assessment").with(user(principal)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.skillsStatus").value("UNKNOWN"))
				.andExpect(jsonPath("$.skillsMatchCount").doesNotExist())
				.andExpect(jsonPath("$.requiredSkillsNotListed").isEmpty())
				.andExpect(jsonPath("$.graduationEligibility").value("UNKNOWN"))
				.andExpect(jsonPath("$.roleAlignment.status").value("UNKNOWN"))
				.andExpect(jsonPath("$.locationPreferenceMatch.status").value("UNKNOWN"))
				.andExpect(jsonPath("$.workModePreferenceMatch.status").value("UNKNOWN"));
	}

	@Test
	void graduationYearBoundsAreInclusive() throws Exception {
		updateProfile(LocalDate.of(2027, 1, 1), List.of(), List.of(), List.of(), List.of());
		mockMvc.perform(get(OPPORTUNITIES_PATH + "/" + opportunityId + "/fit-assessment").with(user(principal)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.graduationEligibility").value("ELIGIBLE"));

		updateProfile(LocalDate.of(2028, 12, 31), List.of(), List.of(), List.of(), List.of());
		mockMvc.perform(get(OPPORTUNITIES_PATH + "/" + opportunityId + "/fit-assessment").with(user(principal)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.graduationEligibility").value("ELIGIBLE"));
	}

	@Test
	void graduationYearOutsideInclusiveBoundsIsIneligible() throws Exception {
		updateProfile(LocalDate.of(2029, 1, 1), List.of(), List.of(), List.of(), List.of());

		mockMvc.perform(get(OPPORTUNITIES_PATH + "/" + opportunityId + "/fit-assessment").with(user(principal)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.graduationEligibility").value("INELIGIBLE"));
	}

	@Test
	void missingGraduationEligibilityBoundReturnsUnknown() throws Exception {
		jdbcTemplate.update(
				"UPDATE opportunity_catalogue SET eligible_graduation_to = NULL WHERE id = ?",
				opportunityId);
		updateProfile(LocalDate.of(2027, 6, 1), List.of(), List.of(), List.of(), List.of());

		mockMvc.perform(get(OPPORTUNITIES_PATH + "/" + opportunityId + "/fit-assessment").with(user(principal)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.graduationEligibility").value("UNKNOWN"));
	}

	@Test
	void nonexistentOpportunityReturnsExistingStructuredNotFoundResponse() throws Exception {
		mockMvc.perform(get(OPPORTUNITIES_PATH + "/" + UUID.randomUUID() + "/fit-assessment").with(user(principal)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Opportunity not found"));
	}

	@Test
	void fitAssessmentRequiresAuthentication() throws Exception {
		mockMvc.perform(get(OPPORTUNITIES_PATH + "/" + opportunityId + "/fit-assessment"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.title").value("Unauthorized"));
	}

	@Test
	void fitAssessmentUsesOnlyAuthenticatedStudentsProfile() throws Exception {
		UUID otherStudentId = UUID.randomUUID();
		jdbcTemplate.update(
				"INSERT INTO users (id, email, display_name, password_hash) VALUES (?, ?, ?, ?)",
				otherStudentId, "other-fit-student@example.test", "Other Student", "test-password-hash");
		jdbcTemplate.update("INSERT INTO student_profiles (user_id) VALUES (?)", otherStudentId);
		updateProfileFor(
				otherStudentId,
				LocalDate.of(2027, 6, 1),
				List.of("Java", "SQL"),
				List.of("Software Engineer Intern"),
				List.of("Boston"),
				List.of("remote"));

		mockMvc.perform(get(OPPORTUNITIES_PATH + "/" + opportunityId + "/fit-assessment")
						.param("userId", otherStudentId.toString())
						.with(user(principal)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.skillsStatus").value("UNKNOWN"))
				.andExpect(jsonPath("$.graduationEligibility").value("UNKNOWN"))
				.andExpect(jsonPath("$.roleAlignment.status").value("UNKNOWN"))
				.andExpect(jsonPath("$.locationPreferenceMatch.status").value("UNKNOWN"))
				.andExpect(jsonPath("$.workModePreferenceMatch.status").value("UNKNOWN"))
				.andExpect(jsonPath("$.university").doesNotExist())
				.andExpect(jsonPath("$.skills").doesNotExist());
	}

	@Test
	void missingProfileUsesExistingProfileNotFoundErrorConvention() throws Exception {
		jdbcTemplate.update("DELETE FROM student_profiles WHERE user_id = ?", studentId);

		mockMvc.perform(get(OPPORTUNITIES_PATH + "/" + opportunityId + "/fit-assessment").with(user(principal)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Student profile not found"));
	}

	private UUID insertOpportunity(List<String> skills, Integer eligibleFrom, Integer eligibleTo) {
		UUID id = UUID.randomUUID();
		jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
			Array skillArray = connection.createArrayOf("text", skills.toArray(String[]::new));
			try (PreparedStatement statement = connection.prepareStatement("""
					INSERT INTO opportunity_catalogue (
						id, company, role_title, role_type, location, work_mode,
						eligible_graduation_from, eligible_graduation_to, required_skills
					) VALUES (?, 'Fit Test Company', 'Software Engineer Intern', 'internship', 'Boston', 'remote',
						?, ?, ?)
					""")) {
				statement.setObject(1, id);
				statement.setObject(2, eligibleFrom);
				statement.setObject(3, eligibleTo);
				statement.setArray(4, skillArray);
				statement.executeUpdate();
				return null;
			} finally {
				skillArray.free();
			}
		});
		return id;
	}

	private void setRequiredSkills(List<String> skills) {
		jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
			Array skillArray = connection.createArrayOf("text", skills.toArray(String[]::new));
			try (PreparedStatement statement = connection.prepareStatement(
					"UPDATE opportunity_catalogue SET required_skills = ? WHERE id = ?")) {
				statement.setArray(1, skillArray);
				statement.setObject(2, opportunityId);
				statement.executeUpdate();
				return null;
			} finally {
				skillArray.free();
			}
		});
	}

	private void updateProfile(
			LocalDate graduationDate,
			List<String> skills,
			List<String> targetRoles,
			List<String> locations,
			List<String> workModes) {
		updateProfileFor(studentId, graduationDate, skills, targetRoles, locations, workModes);
	}

	private void updateProfileFor(
			UUID profileStudentId,
			LocalDate graduationDate,
			List<String> skills,
			List<String> targetRoles,
			List<String> locations,
			List<String> workModes) {
		jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
			Array[] arrays = new Array[4];
			try (PreparedStatement statement = connection.prepareStatement("""
					UPDATE student_profiles SET graduation_date = ?, skills = ?, target_role_categories = ?,
					    preferred_locations = ?, work_modes = ?
					WHERE user_id = ?
					""")) {
				statement.setObject(1, graduationDate);
				arrays[0] = connection.createArrayOf("text", skills.toArray(String[]::new));
				arrays[1] = connection.createArrayOf("text", targetRoles.toArray(String[]::new));
				arrays[2] = connection.createArrayOf("text", locations.toArray(String[]::new));
				arrays[3] = connection.createArrayOf("text", workModes.toArray(String[]::new));
				for (int index = 0; index < arrays.length; index++) {
					statement.setArray(index + 2, arrays[index]);
				}
				statement.setObject(6, profileStudentId);
				statement.executeUpdate();
				return null;
			} finally {
				for (Array array : arrays) {
					if (array != null) {
						array.free();
					}
				}
			}
		});
	}
}
