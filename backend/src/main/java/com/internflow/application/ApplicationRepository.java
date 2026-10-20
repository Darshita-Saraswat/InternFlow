package com.internflow.application;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import com.internflow.application.ApplicationDtos.ApplicationEventResponse;
import com.internflow.application.ApplicationDtos.ApplicationRequest;
import com.internflow.application.ApplicationDtos.ApplicationResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ApplicationRepository {

	private static final RowMapper<ApplicationResponse> APPLICATION_MAPPER = (result, rowNumber) -> {
		java.sql.Array skillArray = result.getArray("required_skills");
		List<String> skills = skillArray == null
				? List.of()
				: Arrays.asList((String[]) skillArray.getArray());
		return new ApplicationResponse(
				result.getObject("id", UUID.class),
				result.getString("company"),
				result.getString("role_title"),
				result.getString("role_type"),
				result.getString("url"),
				result.getString("source"),
				result.getString("location"),
				result.getString("work_mode"),
				result.getObject("saved_on", LocalDate.class),
				result.getObject("deadline", LocalDate.class),
				result.getObject("applied_on", LocalDate.class),
				result.getObject("follow_up_on", LocalDate.class),
				result.getObject("assessment_at", OffsetDateTime.class),
				result.getObject("interview_at", OffsetDateTime.class),
				ApplicationStatus.valueOf(result.getString("status").toUpperCase(Locale.ROOT)),
				ApplicationPriority.valueOf(result.getString("priority").toUpperCase(Locale.ROOT)),
				result.getString("next_action"),
				result.getBigDecimal("compensation_min"),
				result.getBigDecimal("compensation_max"),
				result.getString("compensation_currency"),
				List.copyOf(skills),
				result.getString("notes"),
				result.getString("document_reference"),
				result.getObject("created_at", OffsetDateTime.class),
				result.getObject("updated_at", OffsetDateTime.class));
	};

	private static final RowMapper<ApplicationEventResponse> EVENT_MAPPER = (result, rowNumber) ->
			new ApplicationEventResponse(
					result.getObject("id", UUID.class),
					result.getString("event_type"),
					statusOrNull(result.getString("status_before")),
					statusOrNull(result.getString("status_after")),
					result.getString("description"),
					result.getObject("occurred_at", OffsetDateTime.class));

	private final JdbcTemplate jdbcTemplate;

	public ApplicationRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Optional<ApplicationRecord> findOwned(UUID userId, UUID applicationId) {
		return jdbcTemplate.query(
				"SELECT id, company, role_title, status FROM applications WHERE user_id = ? AND id = ?",
				(result, rowNumber) -> new ApplicationRecord(
						result.getObject("id", UUID.class),
						result.getString("company"),
						result.getString("role_title"),
						ApplicationStatus.valueOf(result.getString("status").toUpperCase(Locale.ROOT))),
				userId,
				applicationId).stream().findFirst();
	}

	public List<ApplicationResponse> findAllOwned(
			UUID userId,
			ApplicationStatus status,
			String query,
			int page,
			int size) {
		StringBuilder sql = new StringBuilder("SELECT * FROM applications WHERE user_id = ?");
		List<Object> parameters = new ArrayList<>();
		parameters.add(userId);
		if (status != null) {
			sql.append(" AND status = ?");
			parameters.add(status.name().toLowerCase(Locale.ROOT));
		}
		if (query != null && !query.isBlank()) {
			sql.append(" AND (company ILIKE ? OR role_title ILIKE ?)");
			String pattern = "%" + query.trim() + "%";
			parameters.add(pattern);
			parameters.add(pattern);
		}
		sql.append(" ORDER BY created_at DESC, id LIMIT ? OFFSET ?");
		parameters.add(size);
		parameters.add((long) page * size);
		return jdbcTemplate.query(sql.toString(), APPLICATION_MAPPER, parameters.toArray());
	}

	public long countOwned(UUID userId, ApplicationStatus status, String query) {
		StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM applications WHERE user_id = ?");
		List<Object> parameters = new ArrayList<>();
		parameters.add(userId);
		if (status != null) {
			sql.append(" AND status = ?");
			parameters.add(status.name().toLowerCase(Locale.ROOT));
		}
		if (query != null && !query.isBlank()) {
			sql.append(" AND (company ILIKE ? OR role_title ILIKE ?)");
			String pattern = "%" + query.trim() + "%";
			parameters.add(pattern);
			parameters.add(pattern);
		}
		return jdbcTemplate.queryForObject(sql.toString(), Long.class, parameters.toArray());
	}

	public Optional<ApplicationResponse> findDetailsOwned(UUID userId, UUID applicationId) {
		return jdbcTemplate.query(
				"SELECT * FROM applications WHERE user_id = ? AND id = ?",
				APPLICATION_MAPPER,
				userId,
				applicationId).stream().findFirst();
	}

	public List<ApplicationEventResponse> findEventsOwned(UUID userId, UUID applicationId) {
		return jdbcTemplate.query(
				"SELECT id, event_type, status_before, status_after, description, occurred_at "
						+ "FROM application_events WHERE user_id = ? AND application_id = ? "
						+ "ORDER BY occurred_at DESC, id DESC",
				EVENT_MAPPER,
				userId,
				applicationId);
	}

	public ApplicationResponse createOwned(UUID userId, ApplicationRequest request) {
		String sql = """
				INSERT INTO applications (
					user_id, company, role_title, role_type, url, source, location, work_mode,
					saved_on, deadline, applied_on, follow_up_on, assessment_at, interview_at,
					status, priority, next_action, compensation_min, compensation_max,
					compensation_currency, required_skills, notes, document_reference
				) VALUES (
					?, ?, ?, ?, ?, ?, ?, ?,
					?, ?, ?, ?, ?, ?,
					?, ?, ?,
					?, ?, ?,
					?::text[], ?, ?
				)
				RETURNING *
				""";
		return jdbcTemplate.query(connection -> {
			PreparedStatement statement = connection.prepareStatement(sql);
			bindRequest(statement, userId, request, 1, true);
			return statement;
		}, APPLICATION_MAPPER).stream().findFirst().orElseThrow();
	}

	public boolean updateOwned(UUID userId, UUID applicationId, ApplicationRequest request) {
		String sql = """
				UPDATE applications SET
					company = ?, role_title = ?, role_type = ?, url = ?, source = ?, location = ?,
					work_mode = ?, saved_on = ?, deadline = ?, applied_on = ?, follow_up_on = ?,
					assessment_at = ?, interview_at = ?, status = ?, priority = ?, next_action = ?,
					compensation_min = ?, compensation_max = ?, compensation_currency = ?,
					required_skills = ?::text[], notes = ?, document_reference = ?,
					updated_at = CURRENT_TIMESTAMP
				WHERE user_id = ? AND id = ?
				""";
		return jdbcTemplate.update(connection -> {
			PreparedStatement statement = connection.prepareStatement(sql);
			bindRequest(statement, userId, request, 1, false);
			statement.setObject(23, userId);
			statement.setObject(24, applicationId);
			return statement;
		}) == 1;
	}

	public void insertEvent(
			UUID userId,
			UUID applicationId,
			String eventType,
			ApplicationStatus statusBefore,
			ApplicationStatus statusAfter,
			String description) {
		jdbcTemplate.update(
				"INSERT INTO application_events "
						+ "(user_id, application_id, event_type, status_before, status_after, description, occurred_at) "
						+ "VALUES (?, ?, ?, ?, ?, ?, clock_timestamp())",
				userId,
				applicationId,
				eventType,
				statusBefore == null ? null : statusBefore.name().toLowerCase(Locale.ROOT),
				statusAfter == null ? null : statusAfter.name().toLowerCase(Locale.ROOT),
				description);
	}

	public boolean updateStatus(UUID userId, UUID applicationId, ApplicationStatus status) {
		return jdbcTemplate.update(
				"UPDATE applications SET status = ?, updated_at = CURRENT_TIMESTAMP "
						+ "WHERE user_id = ? AND id = ?",
				status.name().toLowerCase(Locale.ROOT),
				userId,
				applicationId) == 1;
	}

	public boolean deleteOwned(UUID userId, UUID applicationId) {
		return jdbcTemplate.update(
				"DELETE FROM applications WHERE user_id = ? AND id = ?",
				userId,
				applicationId) == 1;
	}

	public record ApplicationRecord(UUID id, String company, String roleTitle, ApplicationStatus status) {
	}

	private static void bindRequest(
			PreparedStatement statement,
			UUID userId,
			ApplicationRequest request,
			int startIndex,
			boolean includeUserId) throws SQLException {
		int index = startIndex;
		if (includeUserId) {
			statement.setObject(index++, userId);
		}
		statement.setString(index++, request.company().trim());
		statement.setString(index++, request.roleTitle().trim());
		statement.setString(index++, request.roleType());
		statement.setString(index++, clean(request.url()));
		statement.setString(index++, clean(request.source()));
		statement.setString(index++, clean(request.location()));
		statement.setString(index++, clean(request.workMode()));
		statement.setObject(index++, request.savedOn());
		statement.setObject(index++, request.deadline());
		statement.setObject(index++, request.appliedOn());
		statement.setObject(index++, request.followUpOn());
		statement.setObject(index++, request.assessmentAt());
		statement.setObject(index++, request.interviewAt());
		statement.setString(index++, request.status().name().toLowerCase(Locale.ROOT));
		statement.setString(index++, request.priority().name().toLowerCase(Locale.ROOT));
		statement.setString(index++, clean(request.nextAction()));
		statement.setBigDecimal(index++, request.compensationMin());
		statement.setBigDecimal(index++, request.compensationMax());
		statement.setString(index++, clean(request.compensationCurrency()));
		statement.setArray(index++, statement.getConnection().createArrayOf(
				"text",
				request.requiredSkills().stream().map(String::trim).toArray(String[]::new)));
		statement.setString(index++, clean(request.notes()));
		statement.setString(index, clean(request.documentReference()));
	}

	private static String clean(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private static ApplicationStatus statusOrNull(String value) {
		return value == null ? null : ApplicationStatus.valueOf(value.toUpperCase(Locale.ROOT));
	}
}
