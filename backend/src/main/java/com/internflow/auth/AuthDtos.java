package com.internflow.auth;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

	private AuthDtos() {
	}

	public record RegisterRequest(
			@NotBlank @Email @Size(max = 320) String email,
			@NotBlank @Size(min = 2, max = 120) String displayName,
			@NotBlank @Size(min = 12, max = 72) @PasswordByteLength String password) {
	}

	public record LoginRequest(
			@NotBlank @Email @Size(max = 320) String email,
			@NotBlank @Size(max = 72) @PasswordByteLength String password) {
	}

	public record AccountResponse(UUID id, String email, String displayName) {
	}

	public record CsrfResponse(String headerName, String token) {
	}
}
