package com.internflow.profile;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.internflow.profile.StudentProfileDtos.ProfileRequest;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class StudentProfileRepository {

	private static final String SELECT_PROFILE = """
			SELECT university, degree, graduation_date, study_year,
			       target_role_categories, skills, preferred_locations, work_modes
			FROM student_profiles
			WHERE user_id = ?
			""";

	private static final RowMapper<ProfileRecord> PROFILE_MAPPER = (result, rowNumber) -> new ProfileRecord(
			result.getString("university"),
			result.getString("degree"),
			result.getObject("graduation_date", LocalDate.class),
			(Integer) result.getObject("study_year"),
			textArray(result, "target_role_categories"),
			textArray(result, "skills"),
			textArray(result, "preferred_locations"),
			textArray(result, "work_modes"));

	private final JdbcTemplate jdbcTemplate;

	public StudentProfileRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Optional<ProfileRecord> findByUserId(UUID userId) {
		return jdbcTemplate.query(SELECT_PROFILE, PROFILE_MAPPER, userId).stream().findFirst();
	}

	public Optional<ProfileRecord> update(UUID userId, ProfileRequest profile) {
		Boolean updated = jdbcTemplate.execute((ConnectionCallback<Boolean>) connection -> {
			try (PreparedStatement statement = connection.prepareStatement("""
					UPDATE student_profiles
					SET university = ?, degree = ?, graduation_date = ?, study_year = ?,
					    target_role_categories = ?, skills = ?, preferred_locations = ?, work_modes = ?,
					    updated_at = CURRENT_TIMESTAMP
					WHERE user_id = ?
					""")) {
				Array[] arrays = new Array[4];
				try {
					statement.setString(1, profile.university());
					statement.setString(2, profile.degree());
					if (profile.graduationDate() == null) {
						statement.setNull(3, Types.DATE);
					} else {
						statement.setObject(3, profile.graduationDate());
					}
					if (profile.studyYear() == null) {
						statement.setNull(4, Types.SMALLINT);
					} else {
						statement.setShort(4, profile.studyYear().shortValue());
					}
					arrays[0] = connection.createArrayOf("text", profile.targetRoleCategories().toArray(String[]::new));
					arrays[1] = connection.createArrayOf("text", profile.skills().toArray(String[]::new));
					arrays[2] = connection.createArrayOf("text", profile.preferredLocations().toArray(String[]::new));
					arrays[3] = connection.createArrayOf("text", profile.workModes().toArray(String[]::new));
					for (int index = 0; index < arrays.length; index++) {
						statement.setArray(index + 5, arrays[index]);
					}
					statement.setObject(9, userId);
					return statement.executeUpdate() == 1;
				} finally {
					for (Array array : arrays) {
						if (array != null) {
							array.free();
						}
					}
				}
			}
		});
		return Boolean.TRUE.equals(updated) ? findByUserId(userId) : Optional.empty();
	}

	private static List<String> textArray(ResultSet result, String column) throws SQLException {
		Array sqlArray = result.getArray(column);
		if (sqlArray == null) {
			return List.of();
		}
		try {
			Object[] values = (Object[]) sqlArray.getArray();
			return Arrays.stream(values).map(String::valueOf).toList();
		} finally {
			sqlArray.free();
		}
	}

	public record ProfileRecord(
			String university,
			String degree,
			LocalDate graduationDate,
			Integer studyYear,
			List<String> targetRoleCategories,
			List<String> skills,
			List<String> preferredLocations,
			List<String> workModes) {
	}
}
