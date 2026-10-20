package com.internflow.task;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import com.internflow.task.TaskDtos.TaskPageResponse;
import com.internflow.task.TaskDtos.TaskRequest;
import com.internflow.task.TaskDtos.TaskResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class TaskRepository {

	private static final RowMapper<TaskResponse> TASK_MAPPER = (result, rowNumber) -> new TaskResponse(
			result.getObject("id", UUID.class),
			result.getObject("application_id", UUID.class),
			result.getString("application_company"),
			result.getString("application_role_title"),
			result.getString("title"),
			result.getString("description"),
			TaskPriority.valueOf(result.getString("priority").toUpperCase(Locale.ROOT)),
			TaskCategory.valueOf(result.getString("category").toUpperCase(Locale.ROOT)),
			TaskStatus.valueOf(result.getString("status").toUpperCase(Locale.ROOT)),
			result.getObject("due_at", OffsetDateTime.class),
			result.getObject("completed_at", OffsetDateTime.class),
			result.getObject("created_at", OffsetDateTime.class),
			result.getObject("updated_at", OffsetDateTime.class));

	private static final String TASK_SELECT = """
			SELECT t.*, a.company AS application_company, a.role_title AS application_role_title
			FROM tasks t
			LEFT JOIN applications a ON a.user_id = t.user_id AND a.id = t.application_id
			""";

	private final JdbcTemplate jdbcTemplate;

	public TaskRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Optional<TaskResponse> findOwned(UUID userId, UUID taskId) {
		return jdbcTemplate.query(
				TASK_SELECT + " WHERE t.user_id = ? AND t.id = ?",
				TASK_MAPPER,
				userId,
				taskId).stream().findFirst();
	}

	public List<TaskResponse> findAllOwned(TaskFilters filters) {
		QueryParts query = queryParts(filters);
		String sql = TASK_SELECT + query.where()
				+ " ORDER BY " + sortColumn(filters.sort()) + " " + filters.direction().sql()
				+ (filters.sort() == TaskSort.DUE_AT ? " NULLS LAST" : "")
				+ ", t.id LIMIT ? OFFSET ?";
		List<Object> parameters = new ArrayList<>(query.parameters());
		parameters.add(filters.size());
		parameters.add((long) filters.page() * filters.size());
		return jdbcTemplate.query(sql, TASK_MAPPER, parameters.toArray());
	}

	public long countOwned(TaskFilters filters) {
		QueryParts query = queryParts(filters);
		return jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM tasks t" + query.where(),
				Long.class,
				query.parameters().toArray());
	}

	public boolean applicationOwned(UUID userId, UUID applicationId) {
		return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
				"SELECT EXISTS (SELECT 1 FROM applications WHERE user_id = ? AND id = ?)",
				Boolean.class,
				userId,
				applicationId));
	}

	public TaskResponse createOwned(UUID userId, TaskRequest request) {
		String sql = """
				INSERT INTO tasks (
					user_id, application_id, title, description, priority, category, status,
					due_at, completed_at
				) VALUES (
					?, ?, ?, ?, ?, ?, ?, ?,
					CASE WHEN ? = 'completed' THEN clock_timestamp() ELSE NULL END
				)
				RETURNING *
				""";
		UUID taskId = jdbcTemplate.query(connection -> {
			PreparedStatement statement = connection.prepareStatement(sql);
			bindRequest(statement, userId, request, 1, true);
			statement.setString(9, request.status().databaseValue());
			return statement;
		}, (result, rowNumber) -> result.getObject("id", UUID.class))
				.stream().findFirst().orElseThrow();
		return findOwned(userId, taskId).orElseThrow();
	}

	public boolean updateOwned(UUID userId, UUID taskId, TaskRequest request) {
		String sql = """
				UPDATE tasks SET
					application_id = ?, title = ?, description = ?, priority = ?, category = ?,
					status = ?, due_at = ?,
					completed_at = CASE
						WHEN ? = 'completed' THEN COALESCE(completed_at, clock_timestamp())
						ELSE NULL
					END,
					updated_at = clock_timestamp()
				WHERE user_id = ? AND id = ?
				""";
		return jdbcTemplate.update(connection -> {
			PreparedStatement statement = connection.prepareStatement(sql);
			bindRequest(statement, userId, request, 1, false);
			statement.setString(8, request.status().databaseValue());
			statement.setObject(9, userId);
			statement.setObject(10, taskId);
			return statement;
		}) == 1;
	}

	public boolean deleteOwned(UUID userId, UUID taskId) {
		return jdbcTemplate.update(
				"DELETE FROM tasks WHERE user_id = ? AND id = ?",
				userId,
				taskId) == 1;
	}

	public boolean updateTitle(UUID userId, UUID taskId, String title) {
		return jdbcTemplate.update(
				"UPDATE tasks SET title = ?, updated_at = clock_timestamp() WHERE user_id = ? AND id = ?",
				title.trim(),
				userId,
				taskId) == 1;
	}

	private static QueryParts queryParts(TaskFilters filters) {
		StringBuilder where = new StringBuilder(" WHERE t.user_id = ?");
		List<Object> parameters = new ArrayList<>();
		parameters.add(filters.userId());
		if (filters.status() != null) {
			where.append(" AND t.status = ?");
			parameters.add(filters.status().databaseValue());
		}
		if (filters.priority() != null) {
			where.append(" AND t.priority = ?");
			parameters.add(filters.priority().databaseValue());
		}
		if (filters.category() != null) {
			where.append(" AND t.category = ?");
			parameters.add(filters.category().databaseValue());
		}
		if (filters.applicationId() != null) {
			where.append(" AND t.application_id = ?");
			parameters.add(filters.applicationId());
		}
		if (filters.query() != null && !filters.query().isBlank()) {
			where.append(" AND (t.title ILIKE ? OR t.description ILIKE ?)");
			String queryPattern = "%" + filters.query().trim() + "%";
			parameters.add(queryPattern);
			parameters.add(queryPattern);
		}
		if (filters.dueFrom() != null) {
			where.append(" AND t.due_at >= ?");
			parameters.add(filters.dueFrom());
		}
		if (filters.dueTo() != null) {
			where.append(" AND t.due_at <= ?");
			parameters.add(filters.dueTo());
		}
		if (filters.overdue()) {
			where.append(" AND t.status <> 'completed' AND t.due_at < clock_timestamp()");
		}
		return new QueryParts(where.toString(), parameters);
	}

	private static String sortColumn(TaskSort sort) {
		return switch (sort) {
			case DUE_AT -> "t.due_at";
			case CREATED_AT -> "t.created_at";
			case UPDATED_AT -> "t.updated_at";
		};
	}

	private static void bindRequest(
			PreparedStatement statement,
			UUID userId,
			TaskRequest request,
			int startIndex,
			boolean includeUserId) throws SQLException {
		int index = startIndex;
		if (includeUserId) {
			statement.setObject(index++, userId);
		}
		statement.setObject(index++, request.applicationId());
		statement.setString(index++, request.title().trim());
		statement.setString(index++, clean(request.description()));
		statement.setString(index++, request.priority().databaseValue());
		statement.setString(index++, request.category().databaseValue());
		statement.setString(index++, request.status().databaseValue());
		statement.setObject(index++, request.dueAt());
	}

	private static String clean(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private record QueryParts(String where, List<Object> parameters) {
	}

	public record TaskFilters(
			UUID userId,
			TaskStatus status,
			TaskPriority priority,
			TaskCategory category,
			UUID applicationId,
			String query,
			OffsetDateTime dueFrom,
			OffsetDateTime dueTo,
			boolean overdue,
			int page,
			int size,
			TaskSort sort,
			TaskSortDirection direction) {
	}
}
