package com.internflow.application;

import java.net.URI;
import java.util.UUID;

import com.internflow.application.ApplicationDtos.ApplicationDetailResponse;
import com.internflow.application.ApplicationDtos.ApplicationPageResponse;
import com.internflow.application.ApplicationDtos.ApplicationRequest;
import com.internflow.application.ApplicationDtos.ApplicationResponse;
import com.internflow.auth.AccountPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {

	private final ApplicationService applicationService;

	public ApplicationController(ApplicationService applicationService) {
		this.applicationService = applicationService;
	}

	@GetMapping
	ApplicationPageResponse list(
			@AuthenticationPrincipal AccountPrincipal principal,
			@RequestParam(required = false) ApplicationStatus status,
			@RequestParam(required = false) @Size(max = 200) String q,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return applicationService.list(principal.id(), status, q, page, size);
	}

	@PostMapping
	ResponseEntity<ApplicationResponse> create(
			@AuthenticationPrincipal AccountPrincipal principal,
			@Valid @RequestBody ApplicationRequest request) {
		ApplicationResponse created = applicationService.create(principal.id(), request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(created.id())
				.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@GetMapping("/{applicationId}")
	ApplicationDetailResponse get(
			@AuthenticationPrincipal AccountPrincipal principal,
			@PathVariable UUID applicationId) {
		return applicationService.get(principal.id(), applicationId);
	}

	@PutMapping("/{applicationId}")
	ApplicationResponse update(
			@AuthenticationPrincipal AccountPrincipal principal,
			@PathVariable UUID applicationId,
			@Valid @RequestBody ApplicationRequest request) {
		return applicationService.update(principal.id(), applicationId, request);
	}

	@DeleteMapping("/{applicationId}")
	ResponseEntity<Void> delete(
			@AuthenticationPrincipal AccountPrincipal principal,
			@PathVariable UUID applicationId) {
		applicationService.delete(principal.id(), applicationId);
		return ResponseEntity.noContent().build();
	}
}
