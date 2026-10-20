package com.internflow.opportunity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.internflow.opportunity.OpportunityDtos.OpportunityResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class OpportunityRepository {

	private static final String SELECT_COLUMNS = """
			SELECT id, company, role_title, role_type, url, source, location, work_mode, description,
			       deadline, eligible_graduation_from, eligible_graduation_to, required_skills,
			       compensation_min, compensation_max, compensation_currency, created_at, updated_at
			FROM opportunity_catalogue
			""";

	private static final RowMapper<OpportunityResponse> OPPORTUNITY_MAPPER = (result, rowNumber) -> {
		java.sql.Array skillArray = result.getArray("required_skills");
		List<String> skills;
		if (skillArray == null) {
			skills = List.of();
		} else {
			try {
				Object[] values = (Object[]) skillArray.getArray();
				skills = Arrays.stream(values).map(String::valueOf).toList();
			} finally {
				skillArray.free();
			}
		}
		return new OpportunityResponse(
				result.getObject("id", UUID.class),
				result.getString("company"),
				result.getString("role_title"),
				result.getString("role_type"),
				result.getString("url"),
				result.getString("source"),
				result.getString("location"),
				result.getString("work_mode"),
				result.getString("description"),
				result.getObject("deadline", LocalDate.class),
				(Integer) result.getObject("eligible_graduation_from"),
				(Integer) result.getObject("eligible_graduation_to"),
				skills,
				result.getBigDecimal("compensation_min"),
				result.getBigDecimal("compensation_max"),
				result.getString("compensation_currency"),
				result.getObject("created_at", OffsetDateTime.class),
				result.getObject("updated_at", OffsetDateTime.class));
	};

	private final JdbcTemplate jdbcTemplate;

	public OpportunityRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<OpportunityResponse> findAll(
			String query,
			String roleType,
			String location,
			String workMode,
			int page,
			int size) {
		QueryParts queryParts = queryParts(query, roleType, location, workMode);
		List<Object> parameters = new ArrayList<>(queryParts.parameters());
		parameters.add(size);
		parameters.add((long) page * size);
		return jdbcTemplate.query(
				SELECT_COLUMNS + queryParts.where()
						+ " ORDER BY deadline NULLS LAST, company, role_title, id LIMIT ? OFFSET ?",
				OPPORTUNITY_MAPPER,
				parameters.toArray());
	}

	public long count(
			String query,
			String roleType,
			String location,
			String workMode) {
		QueryParts queryParts = queryParts(query, roleType, location, workMode);
		return jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM opportunity_catalogue" + queryParts.where(),
				Long.class,
				queryParts.parameters().toArray());
	}

	public Optional<OpportunityResponse> findById(UUID opportunityId) {
		return jdbcTemplate.query(
				SELECT_COLUMNS + " WHERE id = ?",
				OPPORTUNITY_MAPPER,
				opportunityId).stream().findFirst();
	}

	private static QueryParts queryParts(
			String query,
			String roleType,
			String location,
			String workMode) {
		StringBuilder where = new StringBuilder(" WHERE TRUE");
		List<Object> parameters = new ArrayList<>();
		if (query != null) {
			where.append(" AND (company ILIKE ? OR role_title ILIKE ?)");
			String pattern = "%" + query + "%";
			parameters.add(pattern);
			parameters.add(pattern);
		}
		if (roleType != null) {
			where.append(" AND role_type = ?");
			parameters.add(roleType);
		}
		if (location != null) {
			where.append(" AND location ILIKE ?");
			parameters.add("%" + location + "%");
		}
		if (workMode != null) {
			where.append(" AND work_mode = ?");
			parameters.add(workMode);
		}
		return new QueryParts(where.toString(), parameters);
	}

	private record QueryParts(String where, List<Object> parameters) {
	}
}
