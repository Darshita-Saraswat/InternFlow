package com.internflow.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class ApplicationDtos {

	private ApplicationDtos() {
	}

	public record ApplicationRequest(
			@NotBlank @Size(max = 200) String company,
			@NotBlank @Size(max = 200) String roleTitle,
			@NotBlank @Pattern(regexp = "internship|graduate_role|analyst_role|early_career_other") String roleType,
			@Size(max = 2048) @Pattern(regexp = "(?i)^$|^https?://\\S+$") String url,
			@Pattern(regexp = "|careers_page|campus_placement|referral|job_board|event|other") String source,
			@Size(max = 200) String location,
			@Pattern(regexp = "|in_person|hybrid|remote") String workMode,
			@NotNull LocalDate savedOn,
			LocalDate deadline,
			LocalDate appliedOn,
			LocalDate followUpOn,
			OffsetDateTime assessmentAt,
			OffsetDateTime interviewAt,
			@NotNull ApplicationStatus status,
			@NotNull ApplicationPriority priority,
			@Size(max = 500) String nextAction,
			@DecimalMin("0.00") BigDecimal compensationMin,
			@DecimalMin("0.00") BigDecimal compensationMax,
			@Pattern(regexp = "(?i)^$|^[a-z]{3}$") String compensationCurrency,
			@NotNull @Size(max = 50) List<@NotBlank @Size(max = 80) String> requiredSkills,
			@Size(max = 10000) String notes,
			@Size(max = 2048) String documentReference) {

		@AssertTrue(message = "Minimum compensation must not exceed maximum compensation.")
		public boolean isCompensationRangeValid() {
			return compensationMin == null || compensationMax == null
					|| compensationMin.compareTo(compensationMax) <= 0;
		}
	}

	public record ApplicationResponse(
			UUID id,
			String company,
			String roleTitle,
			String roleType,
			String url,
			String source,
			String location,
			String workMode,
			LocalDate savedOn,
			LocalDate deadline,
			LocalDate appliedOn,
			LocalDate followUpOn,
			OffsetDateTime assessmentAt,
			OffsetDateTime interviewAt,
			ApplicationStatus status,
			ApplicationPriority priority,
			String nextAction,
			BigDecimal compensationMin,
			BigDecimal compensationMax,
			String compensationCurrency,
			List<String> requiredSkills,
			String notes,
			String documentReference,
			OffsetDateTime createdAt,
			OffsetDateTime updatedAt) {
	}

	public record ApplicationEventResponse(
			UUID id,
			String eventType,
			ApplicationStatus statusBefore,
			ApplicationStatus statusAfter,
			String description,
			OffsetDateTime occurredAt) {
	}

	public record ApplicationDetailResponse(
			ApplicationResponse application,
			List<ApplicationEventResponse> events) {
	}

	public record ApplicationPageResponse(
			List<ApplicationResponse> items,
			int page,
			int size,
			long totalItems) {
	}
}
