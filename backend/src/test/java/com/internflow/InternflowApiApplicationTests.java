package com.internflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@Import(InternflowApiApplicationTests.ValidationProbeController.class)
class InternflowApiApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void contextLoads() {
	}

	@Test
	void healthEndpointIsPublic() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.components").doesNotExist());
	}

	@Test
	void openApiDocumentIsPublicAndDescribesTheApi() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title").value("InternFlow API"))
				.andExpect(jsonPath("$.info.version").value("v1"));
	}

	@Test
	void privateApiReturnsStructuredUnauthorizedResponse() throws Exception {
		mockMvc.perform(get("/api/v1/applications"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Unauthorized"))
				.andExpect(jsonPath("$.detail").value("Authentication is required."));
	}

	@Test
	@WithMockUser
	void invalidRequestReturnsFieldSpecificProblemDetail() throws Exception {
		mockMvc.perform(post("/api/v1/test-validation")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\" \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Request validation failed"))
				.andExpect(jsonPath("$.fieldErrors.name[0]").exists());
	}

	@Test
	@WithMockUser
	void malformedJsonReturnsSafeProblemDetail() throws Exception {
		mockMvc.perform(post("/api/v1/test-validation")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Invalid request body"))
				.andExpect(jsonPath("$.detail").value("The request body is missing or malformed."));
	}

	@Test
	void flywayAppliedTheFoundationMigrationAndCreatedExpectedTables() {
		assertThat(jdbcTemplate.queryForObject(
				"SELECT current_setting('server_version_num')::integer", Integer.class))
				.isGreaterThanOrEqualTo(180000);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM flyway_schema_history WHERE success AND version IN ('1', '2', '3')",
				Integer.class))
				.isEqualTo(3);

		List<String> tables = jdbcTemplate.queryForList(
				"SELECT table_name FROM information_schema.tables "
						+ "WHERE table_schema = current_schema() AND table_type = 'BASE TABLE'",
				String.class);
		assertThat(tables).contains(
				"users",
				"student_profiles",
				"opportunities",
				"opportunity_catalogue",
				"applications",
				"application_events",
				"tasks",
				"notification_preferences",
				"spring_session",
				"spring_session_attributes",
				"flyway_schema_history");

		List<String> foreignKeys = jdbcTemplate.queryForList(
				"SELECT conname FROM pg_constraint WHERE contype = 'f' AND convalidated",
				String.class);
		assertThat(foreignKeys).contains(
				"student_profiles_user_fk",
				"opportunities_user_fk",
				"applications_user_fk",
				"applications_opportunity_user_fk",
				"application_events_application_user_fk",
				"tasks_user_fk",
				"tasks_application_user_fk",
				"notification_preferences_user_fk",
				"spring_session_attributes_session_fk");
	}

	@RestController
	static class ValidationProbeController {

		@PostMapping("/api/v1/test-validation")
		void validate(@Valid @RequestBody ValidationProbe request) {
		}
	}

	record ValidationProbe(@NotBlank String name) {
	}

}
