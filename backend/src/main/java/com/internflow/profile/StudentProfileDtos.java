package com.internflow.profile;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class StudentProfileDtos {

	private StudentProfileDtos() {
	}

	public record ProfileRequest(
			@Size(max = 200) String university,
			@Size(max = 200) String degree,
			LocalDate graduationDate,
			@Min(1) @Max(10) Integer studyYear,
			@NotNull @Size(max = 20) List<@NotBlank @Size(max = 80) String> targetRoleCategories,
			@NotNull @Size(max = 50) List<@NotBlank @Size(max = 80) String> skills,
			@NotNull @Size(max = 20) List<@NotBlank @Size(max = 100) String> preferredLocations,
			@NotNull @Size(max = 3) List<@NotBlank @Pattern(regexp = "in_person|hybrid|remote") String> workModes) {
	}

	public record ProfileResponse(
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
