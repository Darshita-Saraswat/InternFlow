package com.internflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import com.internflow.application.ApplicationRepository;
import com.internflow.application.ApplicationDtos;
import com.internflow.application.ApplicationPriority;
import com.internflow.application.ApplicationStatus;
import com.internflow.auth.AuthDtos;
import com.internflow.task.TaskRepository;
import com.internflow.task.TaskCategory;
import com.internflow.task.TaskDtos;
import com.internflow.task.TaskPriority;
import com.internflow.task.TaskStatus;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.util.SerializationUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthIntegrationTests {

	private static final String AUTH_PATH = "/api/v1/auth";
	private static final String VALID_PASSWORD = "InternFlow-secure-2026!";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ApplicationRepository applicationRepository;

	@Autowired
	private TaskRepository taskRepository;

	@Test
	void registrationNormalizesEmailHashesPasswordAndCreatesPrivateDefaults() throws Exception {
		String email = uniqueEmail();
		Csrf csrf = csrfToken(null);

		MvcResult result = mockMvc.perform(post(AUTH_PATH + "/register")
						.cookie(csrf.cookie())
						.header(csrf.headerName(), csrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(new AuthDtos.RegisterRequest(
								email.toUpperCase(),
								"  Ada Student  ",
								VALID_PASSWORD))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.displayName").value("Ada Student"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andReturn();

		UUID accountId = objectMapper.readValue(result.getResponse().getContentAsString(), AuthDtos.AccountResponse.class).id();
		String passwordHash = jdbcTemplate.queryForObject(
				"SELECT password_hash FROM users WHERE id = ?", String.class, accountId);
		assertThat(passwordHash).startsWith("$2a$12$").isNotEqualTo(VALID_PASSWORD);
		assertThat(passwordEncoder.matches(VALID_PASSWORD, passwordHash)).isTrue();
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM student_profiles WHERE user_id = ?", Integer.class, accountId)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM notification_preferences WHERE user_id = ?", Integer.class, accountId))
				.isEqualTo(1);
	}

	@Test
	void duplicateRegistrationReturnsConflictWithoutLeakingDatabaseDetails() throws Exception {
		String email = uniqueEmail();
		register(email);

		Csrf csrf = csrfToken(null);
		MvcResult result = mockMvc.perform(post(AUTH_PATH + "/register")
						.cookie(csrf.cookie())
						.header(csrf.headerName(), csrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(new AuthDtos.RegisterRequest(email.toUpperCase(), "Another Student", VALID_PASSWORD))))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Account already exists"))
				.andExpect(jsonPath("$.detail").value("An account with this email address already exists."))
				.andReturn();
		assertThat(result.getResponse().getContentAsString()).doesNotContain("users_email_unique_idx");
	}

	@Test
	void validLoginPersistsSessionAndCurrentAccountComesFromAuthenticatedPrincipal() throws Exception {
		String email = uniqueEmail();
		register(email);

		Login login = login(email, VALID_PASSWORD);
		assertThat(login.response().getResponse().getStatus()).isEqualTo(200);
		assertThat(login.response().getResponse().getHeaders("Set-Cookie"))
				.anyMatch(value -> value.startsWith("INTERNFLOW_SESSION=")
						&& value.contains("HttpOnly")
						&& value.contains("SameSite=Lax"));

		mockMvc.perform(get(AUTH_PATH + "/me").cookie(login.sessionCookie()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.id").value(login.account().id().toString()))
				.andExpect(jsonPath("$.password").doesNotExist());

		byte[] securityContextBytes = jdbcTemplate.queryForObject(
				"SELECT attribute_bytes FROM spring_session_attributes "
						+ "WHERE session_primary_id = (SELECT primary_id FROM spring_session WHERE session_id = ?) "
						+ "AND attribute_name = 'SPRING_SECURITY_CONTEXT'",
				byte[].class,
				cookieValue(login.sessionCookie()));
		SecurityContext storedContext = (SecurityContext) SerializationUtils.deserialize(securityContextBytes);
		assertThat(((com.internflow.auth.AccountPrincipal) storedContext.getAuthentication().getPrincipal())
				.getPassword()).isNull();

		logout(login.sessionCookie());
	}

	@Test
	void invalidCredentialsReturnTheSameGenericResponseForUnknownAndWrongPassword() throws Exception {
		String email = uniqueEmail();
		register(email);

		MvcResult wrongPassword = login(email, "Wrong-password-2026!").response();
		MvcResult unknownEmail = login(uniqueEmail(), VALID_PASSWORD).response();

		assertThat(wrongPassword.getResponse().getStatus()).isEqualTo(401);
		assertThat(unknownEmail.getResponse().getStatus()).isEqualTo(401);
		assertThat(wrongPassword.getResponse().getContentAsString()).contains("Email or password is incorrect.");
		assertThat(unknownEmail.getResponse().getContentAsString()).contains("Email or password is incorrect.");
		assertThat(wrongPassword.getResponse().getContentAsString()).doesNotContain(email);
	}

	@Test
	void logoutInvalidatesThePersistedSessionAndRejectsTheOldCookie() throws Exception {
		String email = uniqueEmail();
		register(email);
		Login login = login(email, VALID_PASSWORD);
		String sessionId = cookieValue(login.sessionCookie());
		assertThat(sessionCount(sessionId)).isEqualTo(1);

		logout(login.sessionCookie());

		assertThat(sessionCount(sessionId)).isZero();
		mockMvc.perform(get(AUTH_PATH + "/me").cookie(login.sessionCookie()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void csrfCookieIsReadableForDoubleSubmitButSessionCookieIsHttpOnlyAndSameSiteLax() throws Exception {
		MvcResult csrfResponse = mockMvc.perform(get(AUTH_PATH + "/csrf"))
				.andExpect(status().isOk())
				.andReturn();
		Cookie csrfCookie = csrfResponse.getResponse().getCookie("XSRF-TOKEN");
		assertThat(csrfCookie).isNotNull();
		assertThat(csrfCookie.isHttpOnly()).isFalse();
		assertThat(csrfCookie.getAttribute("SameSite")).isEqualTo("Lax");

		String email = uniqueEmail();
		register(email);
		Login login = login(email, VALID_PASSWORD);
		assertThat(login.response().getResponse().getHeaders("Set-Cookie"))
				.anyMatch(value -> value.startsWith("INTERNFLOW_SESSION=")
						&& value.contains("HttpOnly")
						&& value.contains("SameSite=Lax")
						&& !value.contains("Secure"));
		logout(login.sessionCookie());
	}

	@Test
	void stateChangingAuthenticationRequestsRequireValidCsrfToken() throws Exception {
		Csrf csrf = csrfToken(null);
		mockMvc.perform(post(AUTH_PATH + "/register")
						.cookie(csrf.cookie())
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(new AuthDtos.RegisterRequest(uniqueEmail(), "Ada Student", VALID_PASSWORD))))
				.andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Forbidden"));

		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isZero();
	}

	@Test
	void invalidRegistrationFieldsReturnSafeFieldErrors() throws Exception {
		Csrf csrf = csrfToken(null);
		mockMvc.perform(post(AUTH_PATH + "/register")
						.cookie(csrf.cookie())
						.header(csrf.headerName(), csrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"not-an-email","displayName":" ","password":"short"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Request validation failed"))
				.andExpect(jsonPath("$.fieldErrors.email").exists())
				.andExpect(jsonPath("$.fieldErrors.displayName").exists())
				.andExpect(jsonPath("$.fieldErrors.password").exists());

		Csrf longUtf8PasswordCsrf = csrfToken(null);
		mockMvc.perform(post(AUTH_PATH + "/register")
						.cookie(longUtf8PasswordCsrf.cookie())
						.header(longUtf8PasswordCsrf.headerName(), longUtf8PasswordCsrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(new AuthDtos.RegisterRequest(
								uniqueEmail(),
								"Ada Student",
								"😀".repeat(36)))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.password[0]").value("Password must not exceed 72 UTF-8 bytes."));
	}

	@Test
	void userCannotReadUpdateOrDeleteAnotherUsersApplicationOrTask() {
		UUID userA = createUser();
		UUID userB = createUser();
		UUID applicationId = createApplication(userB);
		UUID taskId = createTask(userB, applicationId);

		assertThat(applicationRepository.findOwned(userA, applicationId)).isEmpty();
		assertThat(applicationRepository.updateStatus(userA, applicationId, ApplicationStatus.INTERVIEW)).isFalse();
		assertThat(applicationRepository.deleteOwned(userA, applicationId)).isFalse();
		assertThat(applicationRepository.findOwned(userB, applicationId))
				.get()
				.extracting(ApplicationRepository.ApplicationRecord::status)
				.isEqualTo(ApplicationStatus.SAVED);

		assertThat(taskRepository.findOwned(userA, taskId)).isEmpty();
		TaskDtos.TaskRequest unauthorizedUpdate = taskRequest(
				"Tampered task", TaskStatus.COMPLETED, TaskCategory.OTHER, null, null);
		assertThat(taskRepository.updateOwned(userA, taskId, unauthorizedUpdate)).isFalse();
		assertThat(taskRepository.deleteOwned(userA, taskId)).isFalse();
		assertThat(taskRepository.findOwned(userB, taskId))
				.get()
				.extracting(TaskDtos.TaskResponse::title)
				.isEqualTo("Prepare interview notes");
	}

	@Test
	void taskApiPersistsCrudFiltersDeadlinesAndCompletion() throws Exception {
		AuthDtos.AccountResponse account = register(uniqueEmail());
		Login login = login(account.email(), VALID_PASSWORD);
		Csrf csrf = csrfToken(login.sessionCookie());
		UUID applicationId = createApplication(account.id());
		java.time.OffsetDateTime dueAt = java.time.OffsetDateTime.now().plusDays(2).withNano(0);
		TaskDtos.TaskRequest request = taskRequest(
				"Prepare interview", TaskStatus.TODO, TaskCategory.INTERVIEW_PREPARATION, dueAt, applicationId);

		mockMvc.perform(post("/api/v1/tasks")
						.cookie(login.sessionCookie(), csrf.cookie())
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(request)))
				.andExpect(status().isForbidden());

		MvcResult createdResult = mockMvc.perform(post("/api/v1/tasks")
						.cookie(login.sessionCookie(), csrf.cookie())
						.header(csrf.headerName(), csrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.title").value("Prepare interview"))
				.andExpect(jsonPath("$.status").value("TODO"))
				.andExpect(jsonPath("$.applicationId").value(applicationId.toString()))
				.andReturn();
		TaskDtos.TaskResponse created = objectMapper.readValue(
				createdResult.getResponse().getContentAsString(), TaskDtos.TaskResponse.class);

		mockMvc.perform(post("/api/v1/tasks")
						.cookie(login.sessionCookie(), csrf.cookie())
						.header(csrf.headerName(), csrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(taskRequest(
								"Finish application draft",
								TaskStatus.TODO,
								TaskCategory.RESEARCH,
								dueAt.minusDays(1),
								null))))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/v1/tasks")
						.cookie(login.sessionCookie(), csrf.cookie())
						.header(csrf.headerName(), csrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(taskRequest(
								"Send overdue follow-up",
								TaskStatus.IN_PROGRESS,
								TaskCategory.FOLLOW_UP,
								java.time.OffsetDateTime.now().minusDays(1).withNano(0),
								null))))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/tasks")
						.cookie(login.sessionCookie())
						.param("status", "TODO")
						.param("category", "INTERVIEW_PREPARATION")
						.param("applicationId", applicationId.toString())
						.param("dueFrom", dueAt.minusDays(1).toString())
						.param("dueTo", dueAt.plusDays(1).toString())
						.param("sort", "DUE_AT")
						.param("direction", "ASC"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalItems").value(1))
				.andExpect(jsonPath("$.items[0].id").value(created.id().toString()));

		mockMvc.perform(get("/api/v1/tasks")
						.cookie(login.sessionCookie())
						.param("q", "interview"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalItems").value(1))
				.andExpect(jsonPath("$.items[0].id").value(created.id().toString()));

		mockMvc.perform(get("/api/v1/tasks")
						.cookie(login.sessionCookie())
						.param("overdue", "true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalItems").value(1))
				.andExpect(jsonPath("$.items[0].title").value("Send overdue follow-up"));
		mockMvc.perform(get("/api/v1/tasks")
						.cookie(login.sessionCookie())
						.param("sort", "DUE_AT")
						.param("direction", "ASC"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].title").value("Send overdue follow-up"))
				.andExpect(jsonPath("$.items[1].title").value("Finish application draft"))
				.andExpect(jsonPath("$.items[2].title").value("Prepare interview"));

		Csrf updateCsrf = csrfToken(login.sessionCookie());
		mockMvc.perform(put("/api/v1/tasks/{id}", created.id())
						.cookie(login.sessionCookie(), updateCsrf.cookie())
						.header(updateCsrf.headerName(), updateCsrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(taskRequest(
								"Prepare interview", TaskStatus.COMPLETED,
								TaskCategory.INTERVIEW_PREPARATION, dueAt, applicationId))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("COMPLETED"))
				.andExpect(jsonPath("$.completedAt").isNotEmpty());

		mockMvc.perform(get("/api/v1/tasks/{id}", created.id())
						.cookie(login.sessionCookie()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.applicationCompany").value("Example Co"));

		Csrf deleteCsrf = csrfToken(login.sessionCookie());
		mockMvc.perform(delete("/api/v1/tasks/{id}", created.id())
						.cookie(login.sessionCookie(), deleteCsrf.cookie())
						.header(deleteCsrf.headerName(), deleteCsrf.token()))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/tasks/{id}", created.id())
						.cookie(login.sessionCookie()))
				.andExpect(status().isNotFound());
		logout(login.sessionCookie());
	}

	@Test
	void taskApiEnforcesAuthenticationCsrfAndCrossUserOwnership() throws Exception {
		mockMvc.perform(get("/api/v1/tasks"))
				.andExpect(status().isUnauthorized());

		AuthDtos.AccountResponse userA = register(uniqueEmail());
		Login loginA = login(userA.email(), VALID_PASSWORD);
		AuthDtos.AccountResponse userB = register(uniqueEmail());
		UUID foreignApplicationId = createApplication(userB.id());
		UUID foreignTaskId = createTask(userB.id(), foreignApplicationId);

		mockMvc.perform(get("/api/v1/tasks")
						.cookie(loginA.sessionCookie()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalItems").value(0));
		mockMvc.perform(get("/api/v1/tasks/{id}", foreignTaskId)
						.cookie(loginA.sessionCookie()))
				.andExpect(status().isNotFound());

		Csrf csrf = csrfToken(loginA.sessionCookie());
		mockMvc.perform(put("/api/v1/tasks/{id}", foreignTaskId)
						.cookie(loginA.sessionCookie(), csrf.cookie())
						.header(csrf.headerName(), csrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(taskRequest(
								"Tampered task", TaskStatus.COMPLETED, TaskCategory.OTHER, null, null))))
				.andExpect(status().isNotFound());
		Csrf deleteCsrf = csrfToken(loginA.sessionCookie());
		mockMvc.perform(delete("/api/v1/tasks/{id}", foreignTaskId)
						.cookie(loginA.sessionCookie(), deleteCsrf.cookie())
						.header(deleteCsrf.headerName(), deleteCsrf.token()))
				.andExpect(status().isNotFound());
		Csrf linkCsrf = csrfToken(loginA.sessionCookie());
		mockMvc.perform(post("/api/v1/tasks")
						.cookie(loginA.sessionCookie(), linkCsrf.cookie())
						.header(linkCsrf.headerName(), linkCsrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(taskRequest(
								"Foreign link", TaskStatus.TODO, TaskCategory.OTHER, null, foreignApplicationId))))
				.andExpect(status().isNotFound());
		mockMvc.perform(post("/api/v1/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(taskRequest(
								"Unauthenticated", TaskStatus.TODO, TaskCategory.OTHER, null, null))))
				.andExpect(status().isForbidden());

		assertThat(jdbcTemplate.queryForObject(
				"SELECT title FROM tasks WHERE id = ? AND user_id = ?",
				String.class,
				foreignTaskId,
				userB.id())).isEqualTo("Prepare interview notes");
		logout(loginA.sessionCookie());
	}

	@Test
	void taskApiReturnsValidationAndFilterErrors() throws Exception {
		AuthDtos.AccountResponse account = register(uniqueEmail());
		Login login = login(account.email(), VALID_PASSWORD);
		Csrf csrf = csrfToken(login.sessionCookie());
		mockMvc.perform(post("/api/v1/tasks")
						.cookie(login.sessionCookie(), csrf.cookie())
						.header(csrf.headerName(), csrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(taskRequest(
								"Flexible reading", TaskStatus.TODO, TaskCategory.OTHER, null, null))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.dueAt").value(org.hamcrest.Matchers.nullValue()))
				.andExpect(jsonPath("$.applicationId").value(org.hamcrest.Matchers.nullValue()));
		mockMvc.perform(post("/api/v1/tasks")
						.cookie(login.sessionCookie(), csrf.cookie())
						.header(csrf.headerName(), csrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":" ","priority":"NORMAL","category":"OTHER","status":"TODO"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Request validation failed"))
				.andExpect(jsonPath("$.fieldErrors.title").exists());

		mockMvc.perform(get("/api/v1/tasks")
						.cookie(login.sessionCookie())
						.param("dueFrom", java.time.OffsetDateTime.now().plusDays(2).toString())
						.param("dueTo", java.time.OffsetDateTime.now().toString()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid task filters"));
		logout(login.sessionCookie());
	}

	@Test
	void applicationApiPersistsCrudFiltersAndStatusActivity() throws Exception {
		AuthDtos.AccountResponse account = register(uniqueEmail());
		Login login = login(account.email(), VALID_PASSWORD);
		Csrf csrf = csrfToken(login.sessionCookie());
		ApplicationDtos.ApplicationRequest request = applicationRequest(
				"Northstar Labs", ApplicationStatus.SAVED);

		MvcResult createdResult = mockMvc.perform(post("/api/v1/applications")
						.cookie(login.sessionCookie(), csrf.cookie())
						.header(csrf.headerName(), csrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.company").value("Northstar Labs"))
				.andExpect(jsonPath("$.status").value("SAVED"))
				.andReturn();
		ApplicationDtos.ApplicationResponse created = objectMapper.readValue(
				createdResult.getResponse().getContentAsString(),
				ApplicationDtos.ApplicationResponse.class);

		mockMvc.perform(get("/api/v1/applications")
						.cookie(login.sessionCookie())
						.param("q", "northstar")
						.param("status", "SAVED"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalItems").value(1))
				.andExpect(jsonPath("$.items[0].id").value(created.id().toString()));

		Csrf updateCsrf = csrfToken(login.sessionCookie());
		mockMvc.perform(put("/api/v1/applications/{id}", created.id())
						.cookie(login.sessionCookie(), updateCsrf.cookie())
						.header(updateCsrf.headerName(), updateCsrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(applicationRequest("Northstar Labs", ApplicationStatus.INTERVIEW))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("INTERVIEW"));

		mockMvc.perform(get("/api/v1/applications/{id}", created.id())
						.cookie(login.sessionCookie()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.events.length()").value(2))
				.andExpect(jsonPath("$.events[0].eventType").value("status_changed"))
				.andExpect(jsonPath("$.events[0].statusBefore").value("SAVED"))
				.andExpect(jsonPath("$.events[0].statusAfter").value("INTERVIEW"));

		Csrf deleteCsrf = csrfToken(login.sessionCookie());
		mockMvc.perform(delete("/api/v1/applications/{id}", created.id())
						.cookie(login.sessionCookie(), deleteCsrf.cookie())
						.header(deleteCsrf.headerName(), deleteCsrf.token()))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/applications/{id}", created.id())
						.cookie(login.sessionCookie()))
				.andExpect(status().isNotFound());
		logout(login.sessionCookie());
	}

	@Test
	void applicationApiHidesAnotherUsersRecordsAndRejectsUnauthenticatedAccess() throws Exception {
		mockMvc.perform(get("/api/v1/applications"))
				.andExpect(status().isUnauthorized());

		AuthDtos.AccountResponse userA = register(uniqueEmail());
		Login loginA = login(userA.email(), VALID_PASSWORD);
		AuthDtos.AccountResponse userB = register(uniqueEmail());
		UUID applicationId = createApplication(userB.id());

		mockMvc.perform(get("/api/v1/applications")
						.cookie(loginA.sessionCookie()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalItems").value(0));

		mockMvc.perform(get("/api/v1/applications/{id}", applicationId)
						.cookie(loginA.sessionCookie()))
				.andExpect(status().isNotFound());

		Csrf csrf = csrfToken(loginA.sessionCookie());
		mockMvc.perform(put("/api/v1/applications/{id}", applicationId)
						.cookie(loginA.sessionCookie(), csrf.cookie())
						.header(csrf.headerName(), csrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(applicationRequest("Changed by user A", ApplicationStatus.REJECTED))))
				.andExpect(status().isNotFound());

		Csrf deleteCsrf = csrfToken(loginA.sessionCookie());
		mockMvc.perform(delete("/api/v1/applications/{id}", applicationId)
						.cookie(loginA.sessionCookie(), deleteCsrf.cookie())
						.header(deleteCsrf.headerName(), deleteCsrf.token()))
				.andExpect(status().isNotFound());

		assertThat(jdbcTemplate.queryForObject(
				"SELECT company FROM applications WHERE id = ? AND user_id = ?",
				String.class,
				applicationId,
				userB.id())).isEqualTo("Example Co");
		logout(loginA.sessionCookie());
	}

	@Test
	void applicationApiReturnsFieldErrorsForInvalidInput() throws Exception {
		AuthDtos.AccountResponse account = register(uniqueEmail());
		Login login = login(account.email(), VALID_PASSWORD);
		Csrf csrf = csrfToken(login.sessionCookie());
		ApplicationDtos.ApplicationRequest request = new ApplicationDtos.ApplicationRequest(
				" ", "Role", "invalid_role", null, null, null, null,
				java.time.LocalDate.now(), null, null, null, null, null,
				ApplicationStatus.SAVED, ApplicationPriority.NORMAL, null, null, null, null,
				java.util.List.of(), null, null);

		mockMvc.perform(post("/api/v1/applications")
						.cookie(login.sessionCookie(), csrf.cookie())
						.header(csrf.headerName(), csrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Request validation failed"))
				.andExpect(jsonPath("$.fieldErrors.company").exists())
				.andExpect(jsonPath("$.fieldErrors.roleType").exists());
		logout(login.sessionCookie());
	}

	private AuthDtos.AccountResponse register(String email) throws Exception {
		Csrf csrf = csrfToken(null);
		MvcResult result = mockMvc.perform(post(AUTH_PATH + "/register")
						.cookie(csrf.cookie())
						.header(csrf.headerName(), csrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(new AuthDtos.RegisterRequest(email, "Ada Student", VALID_PASSWORD))))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readValue(result.getResponse().getContentAsString(), AuthDtos.AccountResponse.class);
	}

	private Login login(String email, String password) throws Exception {
		Csrf csrf = csrfToken(null);
		MvcResult response = mockMvc.perform(post(AUTH_PATH + "/login")
						.cookie(csrf.cookie())
						.header(csrf.headerName(), csrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(new AuthDtos.LoginRequest(email, password))))
				.andReturn();
		AuthDtos.AccountResponse account = response.getResponse().getStatus() == 200
				? objectMapper.readValue(response.getResponse().getContentAsString(), AuthDtos.AccountResponse.class)
				: null;
		Cookie sessionCookie = response.getResponse().getStatus() == 200 ? sessionCookie(response) : null;
		return new Login(response, account, sessionCookie);
	}

	private static Cookie sessionCookie(MvcResult response) {
		String header = response.getResponse().getHeaders("Set-Cookie").stream()
				.filter(value -> value.startsWith("INTERNFLOW_SESSION="))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Session cookie was not returned."));
		String value = header.substring("INTERNFLOW_SESSION=".length(), header.indexOf(';'));
		return new Cookie("INTERNFLOW_SESSION", value);
	}

	private void logout(Cookie sessionCookie) throws Exception {
		Csrf csrf = csrfToken(sessionCookie);
		mockMvc.perform(post(AUTH_PATH + "/logout")
						.cookie(sessionCookie, csrf.cookie())
						.header(csrf.headerName(), csrf.token()))
				.andExpect(status().isNoContent());
	}

	private Csrf csrfToken(Cookie sessionCookie) throws Exception {
		var request = get(AUTH_PATH + "/csrf");
		if (sessionCookie != null) {
			request.cookie(sessionCookie);
		}
		MvcResult response = mockMvc.perform(request)
				.andExpect(status().isOk())
				.andReturn();
		AuthDtos.CsrfResponse csrf = objectMapper.readValue(
				response.getResponse().getContentAsString(), AuthDtos.CsrfResponse.class);
		Cookie cookie = response.getResponse().getCookie("XSRF-TOKEN");
		assertThat(cookie).isNotNull();
		return new Csrf(csrf.headerName(), csrf.token(), cookie);
	}

	private UUID createUser() {
		UUID id = UUID.randomUUID();
		String email = uniqueEmail();
		jdbcTemplate.update(
				"INSERT INTO users (id, email, display_name, password_hash) VALUES (?, ?, ?, ?)",
				id, email, "Fixture User", passwordEncoder.encode(VALID_PASSWORD));
		return id;
	}

	private UUID createApplication(UUID userId) {
		return jdbcTemplate.queryForObject(
				"INSERT INTO applications (user_id, company, role_title, role_type) "
						+ "VALUES (?, 'Example Co', 'Software Intern', 'internship') RETURNING id",
				(result, rowNumber) -> result.getObject(1, UUID.class),
				userId);
	}

	private ApplicationDtos.ApplicationRequest applicationRequest(String company, ApplicationStatus status) {
		return new ApplicationDtos.ApplicationRequest(
				company,
				"Software Engineering Intern",
				"internship",
				"https://example.test/jobs/1",
				"careers_page",
				"Remote",
				"remote",
				java.time.LocalDate.now(),
				null,
				null,
				null,
				null,
				null,
				status,
				ApplicationPriority.NORMAL,
				"Prepare application",
				null,
				null,
				null,
				java.util.List.of("Java", "SQL"),
				"Tracked from the application workflow.",
				null);
	}

	private TaskDtos.TaskRequest taskRequest(
			String title,
			TaskStatus status,
			TaskCategory category,
			java.time.OffsetDateTime dueAt,
			UUID applicationId) {
		return new TaskDtos.TaskRequest(
				title, "Task details", TaskPriority.NORMAL, category, status, dueAt, applicationId);
	}

	private UUID createTask(UUID userId, UUID applicationId) {
		return jdbcTemplate.queryForObject(
				"INSERT INTO tasks (user_id, application_id, title) "
						+ "VALUES (?, ?, 'Prepare interview notes') RETURNING id",
				(result, rowNumber) -> result.getObject(1, UUID.class),
				userId,
				applicationId);
	}

	private int sessionCount(String sessionId) {
		return jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM spring_session WHERE session_id = ?", Integer.class, sessionId);
	}

	private static String cookieValue(Cookie cookie) {
		return new String(Base64.getDecoder().decode(cookie.getValue()), StandardCharsets.UTF_8);
	}

	private String json(Object value) throws Exception {
		return objectMapper.writeValueAsString(value);
	}

	private static String uniqueEmail() {
		return "student-" + UUID.randomUUID() + "@example.test";
	}

	private record Csrf(String headerName, String token, Cookie cookie) {
	}

	private record Login(MvcResult response, AuthDtos.AccountResponse account, Cookie sessionCookie) {
	}
}
