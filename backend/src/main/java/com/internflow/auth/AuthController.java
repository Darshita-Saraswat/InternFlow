package com.internflow.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@GetMapping("/csrf")
	AuthDtos.CsrfResponse csrf(CsrfToken csrfToken) {
		return new AuthDtos.CsrfResponse(csrfToken.getHeaderName(), csrfToken.getToken());
	}

	@PostMapping("/register")
	ResponseEntity<AuthDtos.AccountResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
	}

	@PostMapping("/login")
	AuthDtos.AccountResponse login(
			@Valid @RequestBody AuthDtos.LoginRequest request,
			HttpServletRequest servletRequest,
			HttpServletResponse servletResponse) {
		return authService.login(request, servletRequest, servletResponse);
	}

	@GetMapping("/me")
	AuthDtos.AccountResponse currentAccount(@AuthenticationPrincipal AccountPrincipal principal) {
		return authService.currentAccount(principal);
	}
}
