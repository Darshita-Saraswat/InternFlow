package com.internflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.internflow.auth.AuthDtos;
import com.internflow.profile.StudentProfileDtos.ProfileRequest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StudentProfileIntegrationTests {

	private static final String AUTH_PATH = "/api/v1/auth";
	private static final String PROFILE_PATH = "/api/v1/profile";
	private static final String PASSWORD = "InternFlow-profile-2026!";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void unauthenticatedUserCannotReadOrUpdateProfile() throws Exception {
		Csrf csrf = csrfToken(null);
		mockMvc.perform(get(PROFILE_PATH))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(put(PROFILE_PATH)
						.cookie(csrf.cookie())
						.header(csrf.headerName(), csrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(validProfile())))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void authenticatedUserCanReadDefaultsAndPersistProfileUpdates() throws Exception {
		Login login = createAndLogin();
		mockMvc.perform(get(PROFILE_PATH).cookie(login.sessionCookie()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.university").doesNotExist())
				.andExpect(jsonPath("$.skills").isEmpty())
				.andExpect(jsonPath("$.workModes").isEmpty());

		update(login, validProfile())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.university").value("North University"))
				.andExpect(jsonPath("$.degree").value("Computer Science"))
				.andExpect(jsonPath("$.graduationDate").value("2027-06-01"))
				.andExpect(jsonPath("$.studyYear").value(3))
				.andExpect(jsonPath("$.targetRoleCategories[0]").value("Software Engineering"))
				.andExpect(jsonPath("$.skills[1]").value("SQL"))
				.andExpect(jsonPath("$.preferredLocations[0]").value("Boston"))
				.andExpect(jsonPath("$.workModes[0]").value("hybrid"));

		mockMvc.perform(get(PROFILE_PATH).cookie(login.sessionCookie()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.university").value("North University"))
				.andExpect(jsonPath("$.skills[0]").value("Java"));
		assertThat(jdbcTemplate.queryForObject(
				"SELECT university FROM student_profiles WHERE user_id = ?",
				String.class,
				login.account().id())).isEqualTo("North University");

		update(login, new ProfileRequest(null, null, null, null, List.of(), List.of(), List.of(), List.of()))
				.andExpect(status().isOk());
		assertThat(jdbcTemplate.queryForObject(
				"SELECT university FROM student_profiles WHERE user_id = ?",
				String.class,
				login.account().id())).isNull();
		assertThat(jdbcTemplate.queryForObject(
				"SELECT cardinality(skills) FROM student_profiles WHERE user_id = ?",
				Integer.class,
				login.account().id())).isZero();
	}

	@Test
	void userCannotReadOrModifyAnotherUsersProfile() throws Exception {
		Login userA = createAndLogin();
		Login userB = createAndLogin();
		ObjectNode request = objectMapper.valueToTree(validProfile());
		request.put("userId", userB.account().id().toString());

		update(userA, request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.university").value("North University"));
		mockMvc.perform(get(PROFILE_PATH).cookie(userB.sessionCookie()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.university").doesNotExist())
				.andExpect(jsonPath("$.skills").isEmpty());

		assertThat(jdbcTemplate.queryForObject(
				"SELECT university FROM student_profiles WHERE user_id = ?",
				String.class,
				userA.account().id())).isEqualTo("North University");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT university FROM student_profiles WHERE user_id = ?",
				String.class,
				userB.account().id())).isNull();
	}

	@Test
	void invalidProfileValuesReturnFieldValidationErrorsWithoutPersistence() throws Exception {
		Login login = createAndLogin();
		ProfileRequest invalid = new ProfileRequest(
				"North University",
				"Computer Science",
				LocalDate.of(2027, 6, 1),
				11,
				List.of("Software Engineering"),
				List.of("Java"),
				List.of("Boston"),
				List.of("hybrid"));

		update(login, invalid)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Request validation failed"))
				.andExpect(jsonPath("$.fieldErrors.studyYear[0]").exists());
		assertThat(jdbcTemplate.queryForObject(
				"SELECT university FROM student_profiles WHERE user_id = ?",
				String.class,
				login.account().id())).isNull();
	}

	@Test
	void profileUpdateRequiresCsrfToken() throws Exception {
		Login login = createAndLogin();
		Csrf csrf = csrfToken(login.sessionCookie());

		mockMvc.perform(put(PROFILE_PATH)
						.cookie(login.sessionCookie(), csrf.cookie())
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(validProfile())))
				.andExpect(status().isForbidden());

		mockMvc.perform(put(PROFILE_PATH)
						.cookie(login.sessionCookie(), csrf.cookie())
						.header(csrf.headerName(), csrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(validProfile())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.university").value("North University"));
	}

	private ResultActions update(Login login, Object profile) throws Exception {
		Csrf csrf = csrfToken(login.sessionCookie());
		return mockMvc.perform(put(PROFILE_PATH)
						.cookie(login.sessionCookie(), csrf.cookie())
						.header(csrf.headerName(), csrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(profile)));
	}

	private Login createAndLogin() throws Exception {
		String email = "profile-" + UUID.randomUUID() + "@example.test";
		Csrf registrationCsrf = csrfToken(null);
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(AUTH_PATH + "/register")
						.cookie(registrationCsrf.cookie())
						.header(registrationCsrf.headerName(), registrationCsrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(new AuthDtos.RegisterRequest(email, "Profile Student", PASSWORD))))
				.andExpect(status().isCreated());

		Csrf loginCsrf = csrfToken(null);
		MvcResult response = mockMvc.perform(
						org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(AUTH_PATH + "/login")
								.cookie(loginCsrf.cookie())
								.header(loginCsrf.headerName(), loginCsrf.token())
								.contentType(MediaType.APPLICATION_JSON)
								.content(json(new AuthDtos.LoginRequest(email, PASSWORD))))
				.andExpect(status().isOk())
				.andReturn();
		AuthDtos.AccountResponse account = objectMapper.readValue(
				response.getResponse().getContentAsString(), AuthDtos.AccountResponse.class);
		String sessionHeader = response.getResponse().getHeaders("Set-Cookie").stream()
				.filter(value -> value.startsWith("INTERNFLOW_SESSION="))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Session cookie was not returned."));
		String sessionValue = sessionHeader.substring(
				"INTERNFLOW_SESSION=".length(), sessionHeader.indexOf(';'));
		return new Login(account, new Cookie("INTERNFLOW_SESSION", sessionValue));
	}

	private Csrf csrfToken(Cookie sessionCookie) throws Exception {
		var request = get(AUTH_PATH + "/csrf");
		if (sessionCookie != null) {
			request.cookie(sessionCookie);
		}
		MvcResult response = mockMvc.perform(request)
				.andExpect(status().isOk())
				.andReturn();
		AuthDtos.CsrfResponse token = objectMapper.readValue(
				response.getResponse().getContentAsString(), AuthDtos.CsrfResponse.class);
		return new Csrf(
				token.headerName(),
				token.token(),
				response.getResponse().getCookie("XSRF-TOKEN"));
	}

	private String json(Object value) throws Exception {
		return objectMapper.writeValueAsString(value);
	}

	private static ProfileRequest validProfile() {
		return new ProfileRequest(
				"North University",
				"Computer Science",
				LocalDate.of(2027, 6, 1),
				3,
				List.of("Software Engineering"),
				List.of("Java", "SQL"),
				List.of("Boston"),
				List.of("hybrid"));
	}

	private record Csrf(String headerName, String token, Cookie cookie) {
	}

	private record Login(AuthDtos.AccountResponse account, Cookie sessionCookie) {
	}
}
