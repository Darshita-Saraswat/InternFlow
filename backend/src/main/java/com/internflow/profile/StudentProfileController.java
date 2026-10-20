package com.internflow.profile;

import com.internflow.auth.AccountPrincipal;
import com.internflow.profile.StudentProfileDtos.ProfileRequest;
import com.internflow.profile.StudentProfileDtos.ProfileResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
public class StudentProfileController {

	private final StudentProfileService service;

	public StudentProfileController(StudentProfileService service) {
		this.service = service;
	}

	@GetMapping
	ProfileResponse get(@AuthenticationPrincipal AccountPrincipal principal) {
		return service.get(principal.id());
	}

	@PutMapping
	ProfileResponse update(
			@AuthenticationPrincipal AccountPrincipal principal,
			@Valid @RequestBody ProfileRequest request) {
		return service.update(principal.id(), request);
	}
}
