package com.internflow.auth;

import java.util.Locale;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AccountRepository {

	private static final RowMapper<AccountPrincipal> ACCOUNT_MAPPER = (result, rowNumber) -> new AccountPrincipal(
			result.getObject("id", UUID.class),
			result.getString("email"),
			result.getString("display_name"),
			result.getString("password_hash"));

	private final JdbcTemplate jdbcTemplate;

	public AccountRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public AccountPrincipal create(String email, String displayName, String passwordHash) {
		UUID id = UUID.randomUUID();
		jdbcTemplate.update(
				"INSERT INTO users (id, email, display_name, password_hash) VALUES (?, ?, ?, ?)",
				id,
				normalizeEmail(email),
				displayName.trim(),
				passwordHash);
		jdbcTemplate.update("INSERT INTO student_profiles (user_id) VALUES (?)", id);
		jdbcTemplate.update("INSERT INTO notification_preferences (user_id) VALUES (?)", id);
		return findById(id);
	}

	public AccountPrincipal findByEmail(String email) {
		return jdbcTemplate.queryForObject(
				"SELECT id, email, display_name, password_hash FROM users WHERE email = ?",
				ACCOUNT_MAPPER,
				normalizeEmail(email));
	}

	public AccountPrincipal findById(UUID id) {
		return jdbcTemplate.queryForObject(
				"SELECT id, email, display_name, password_hash FROM users WHERE id = ?",
				ACCOUNT_MAPPER,
				id);
	}

	static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
