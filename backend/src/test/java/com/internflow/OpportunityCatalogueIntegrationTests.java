package com.internflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import com.internflow.auth.AccountPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OpportunityCatalogueIntegrationTests {

	private static final String OPPORTUNITIES_PATH = "/api/v1/opportunities";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private final AccountPrincipal principal = new AccountPrincipal(
			UUID.randomUUID(), "catalogue-student@example.test", "Catalogue Student", "not-a-real-password");

	private UUID remoteInternshipId;

	@BeforeEach
	void insertCatalogueFixtures() {
		remoteInternshipId = insertOpportunity(
				"Acme", "Software Engineer Intern", "internship", "Boston", "remote");
		insertOpportunity("Beta", "Data Analyst", "analyst_role", "Seattle", "hybrid");
		insertOpportunity("Acme Research", "Product Intern", "internship", "Boston", "hybrid");
	}

	@Test
	void migrationCreatesSharedCatalogueWithoutChangingExistingOwnershipSchema() {
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM flyway_schema_history WHERE success AND version = '3'",
				Integer.class)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM information_schema.columns "
						+ "WHERE table_schema = current_schema() AND table_name = 'opportunity_catalogue' "
						+ "AND column_name = 'user_id'",
				Integer.class)).isZero();
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM pg_constraint WHERE conrelid = 'opportunity_catalogue'::regclass "
						+ "AND contype = 'p' AND conkey = ARRAY[("
						+ "SELECT attnum FROM pg_attribute WHERE attrelid = 'opportunity_catalogue'::regclass "
						+ "AND attname = 'id')::smallint]",
				Integer.class)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForList(
				"SELECT indexname FROM pg_indexes WHERE schemaname = current_schema() "
						+ "AND tablename = 'opportunity_catalogue'",
				String.class)).contains(
						"opportunity_catalogue_pkey",
						"opportunity_catalogue_role_type_idx",
						"opportunity_catalogue_location_idx",
						"opportunity_catalogue_work_mode_idx",
						"opportunity_catalogue_deadline_idx");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM pg_constraint WHERE conrelid = 'opportunity_catalogue'::regclass "
						+ "AND contype = 'f'",
				Integer.class)).isZero();
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM pg_constraint WHERE conrelid = 'applications'::regclass "
						+ "AND conname = 'applications_opportunity_user_fk'",
				Integer.class)).isEqualTo(1);
	}

	@Test
	void listSupportsStablePaginationAndReturnsOnlyCatalogueFields() throws Exception {
		mockMvc.perform(get(OPPORTUNITIES_PATH + "?page=0&size=2").with(user(principal)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(2))
				.andExpect(jsonPath("$.totalItems").value(3))
				.andExpect(jsonPath("$.items.length()").value(2))
				.andExpect(jsonPath("$.items[0].company").value("Acme"))
				.andExpect(jsonPath("$.items[0].requiredSkills[0]").value("Java"))
				.andExpect(jsonPath("$.items[0].userId").doesNotExist())
				.andExpect(jsonPath("$.items[0].notes").doesNotExist());

		mockMvc.perform(get(OPPORTUNITIES_PATH + "?page=1&size=2").with(user(principal)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(1))
				.andExpect(jsonPath("$.totalItems").value(3));
	}

	@Test
	void searchAndFiltersCanBeCombined() throws Exception {
		mockMvc.perform(get(OPPORTUNITIES_PATH + "?q=Engineer").with(user(principal)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalItems").value(1))
				.andExpect(jsonPath("$.items[0].roleTitle").value("Software Engineer Intern"));

		mockMvc.perform(get(OPPORTUNITIES_PATH + "?q=Acme").with(user(principal)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalItems").value(2));

		mockMvc.perform(get(OPPORTUNITIES_PATH
						+ "?roleType=internship&location=Boston&workMode=remote").with(user(principal)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalItems").value(1))
				.andExpect(jsonPath("$.items[0].company").value("Acme"));
	}

	@Test
	void detailReturnsOpportunityAndMissingIdHasStructuredNotFoundResponse() throws Exception {
		mockMvc.perform(get(OPPORTUNITIES_PATH + "/" + remoteInternshipId).with(user(principal)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(remoteInternshipId.toString()))
				.andExpect(jsonPath("$.company").value("Acme"))
				.andExpect(jsonPath("$.roleType").value("internship"))
				.andExpect(jsonPath("$.location").value("Boston"))
				.andExpect(jsonPath("$.workMode").value("remote"))
				.andExpect(jsonPath("$.eligibleGraduationFrom").value(2027))
				.andExpect(jsonPath("$.eligibleGraduationTo").value(2028))
				.andExpect(jsonPath("$.description").value("Catalogue fixture description."));

		mockMvc.perform(get(OPPORTUNITIES_PATH + "/" + UUID.randomUUID()).with(user(principal)))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Opportunity not found"));
	}

	@Test
	void catalogueRequiresAuthentication() throws Exception {
		mockMvc.perform(get(OPPORTUNITIES_PATH))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.title").value("Unauthorized"));
		mockMvc.perform(get(OPPORTUNITIES_PATH + "/" + remoteInternshipId))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void invalidPaginationAndFiltersReturnStructuredValidationErrors() throws Exception {
		List<String> invalidQueries = List.of(
				"?page=-1",
				"?size=101",
				"?roleType=not-a-role",
				"?workMode=not-a-mode",
				"?q=" + "x".repeat(201),
				"?location=" + "x".repeat(201));

		for (String query : invalidQueries) {
			mockMvc.perform(get(OPPORTUNITIES_PATH + query).with(user(principal)))
					.andExpect(status().isBadRequest())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
					.andExpect(jsonPath("$.title").value("Request validation failed"));
		}
	}

	private UUID insertOpportunity(
			String company,
			String roleTitle,
			String roleType,
			String location,
			String workMode) {
		UUID id = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO opportunity_catalogue (
					id, company, role_title, role_type, source, location, work_mode, description,
					deadline, eligible_graduation_from, eligible_graduation_to, required_skills,
					compensation_min, compensation_max, compensation_currency
				) VALUES (
					?, ?, ?, ?, 'job_board', ?, ?, 'Catalogue fixture description.',
					DATE '2027-06-01', 2027, 2028, ARRAY['Java', 'SQL']::TEXT[],
					20.00, 30.00, 'USD'
				)
				""", id, company, roleTitle, roleType, location, workMode);
		return id;
	}
}
