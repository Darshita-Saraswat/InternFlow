package com.internflow.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final AccountRepository accountRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final SecurityContextRepository securityContextRepository;

	public AuthService(
			AccountRepository accountRepository,
			PasswordEncoder passwordEncoder,
			AuthenticationManager authenticationManager,
			SecurityContextRepository securityContextRepository) {
		this.accountRepository = accountRepository;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.securityContextRepository = securityContextRepository;
	}

	@Transactional
	public AuthDtos.AccountResponse register(AuthDtos.RegisterRequest request) {
		AccountPrincipal account;
		try {
			account = accountRepository.create(
					request.email(),
					request.displayName(),
					passwordEncoder.encode(request.password()));
		} catch (DuplicateKeyException exception) {
			throw new EmailAlreadyRegisteredException();
		}
		return toResponse(account);
	}

	public AuthDtos.AccountResponse login(
			AuthDtos.LoginRequest request,
			HttpServletRequest servletRequest,
			HttpServletResponse servletResponse) {
		Authentication authentication;
		try {
			authentication = authenticationManager.authenticate(
					UsernamePasswordAuthenticationToken.unauthenticated(
							AccountRepository.normalizeEmail(request.email()),
							request.password()));
		} catch (AuthenticationException exception) {
			throw new InvalidCredentialsException();
		}

		servletRequest.getSession(true);
		try {
			servletRequest.changeSessionId();
		} catch (IllegalStateException exception) {
			throw new AuthenticationServiceException("Unable to rotate the authenticated session.", exception);
		}

		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, servletRequest, servletResponse);
		return toResponse((AccountPrincipal) authentication.getPrincipal());
	}

	public AuthDtos.AccountResponse currentAccount(AccountPrincipal principal) {
		return toResponse(principal);
	}

	private static AuthDtos.AccountResponse toResponse(AccountPrincipal account) {
		return new AuthDtos.AccountResponse(account.id(), account.getUsername(), account.displayName());
	}
}
