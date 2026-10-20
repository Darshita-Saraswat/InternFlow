package com.internflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

import tools.jackson.databind.ObjectMapper;

@Configuration
public class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(12);
	}

	@Bean
	AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(provider);
	}

	@Bean
	SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	@Bean
	CsrfTokenRepository csrfTokenRepository(
			@Value("${server.servlet.session.cookie.secure:false}") boolean secure) {
		CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		repository.setCookieCustomizer(cookie -> cookie
				.path("/")
				.sameSite("Lax")
				.secure(secure));
		return repository;
	}

	@Bean
	CookieSerializer cookieSerializer(
			@Value("${server.servlet.session.cookie.secure:false}") boolean secure) {
		DefaultCookieSerializer serializer = new DefaultCookieSerializer();
		serializer.setCookieName("INTERNFLOW_SESSION");
		serializer.setCookiePath("/");
		serializer.setUseHttpOnlyCookie(true);
		serializer.setUseSecureCookie(secure);
		serializer.setSameSite("Lax");
		return serializer;
	}

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			ObjectMapper objectMapper,
			CsrfTokenRepository csrfTokenRepository,
			SecurityContextRepository securityContextRepository) throws Exception {
		return http
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
						.requestMatchers(HttpMethod.GET, "/v3/api-docs", "/v3/api-docs/**",
								"/swagger-ui.html", "/swagger-ui/**").permitAll()
						.requestMatchers("/api/v1/auth/csrf", "/api/v1/auth/register", "/api/v1/auth/login")
								.permitAll()
						.anyRequest().authenticated())
				.formLogin(formLogin -> formLogin.disable())
				.httpBasic(httpBasic -> httpBasic.disable())
				.csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository))
				.securityContext(securityContext -> securityContext
						.securityContextRepository(securityContextRepository))
				.logout(logout -> logout
						.logoutUrl("/api/v1/auth/logout")
						.logoutSuccessHandler((request, response, authentication) -> response
								.setStatus(HttpStatus.NO_CONTENT.value())))
				.exceptionHandling(exceptionHandling -> exceptionHandling
						.authenticationEntryPoint((request, response, exception) -> {
							response.setStatus(HttpStatus.UNAUTHORIZED.value());
							response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
							ProblemDetail problem = ProblemDetail.forStatusAndDetail(
									HttpStatus.UNAUTHORIZED, "Authentication is required.");
							problem.setTitle("Unauthorized");
							objectMapper.writeValue(response.getOutputStream(), problem);
						})
						.accessDeniedHandler((request, response, exception) -> {
							response.setStatus(HttpStatus.FORBIDDEN.value());
							response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
							ProblemDetail problem = ProblemDetail.forStatusAndDetail(
									HttpStatus.FORBIDDEN, "Request could not be authorized.");
							problem.setTitle("Forbidden");
							objectMapper.writeValue(response.getOutputStream(), problem);
						}))
				.build();
	}
}
