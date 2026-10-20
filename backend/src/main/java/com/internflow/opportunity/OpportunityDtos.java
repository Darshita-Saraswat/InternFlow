package com.internflow.opportunity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class OpportunityDtos {

	private OpportunityDtos() {
	}

	public record OpportunityResponse(
			UUID id,
			String company,
			String roleTitle,
			String roleType,
			String url,
			String source,
			String location,
			String workMode,
			String description,
			LocalDate deadline,
			Integer eligibleGraduationFrom,
			Integer eligibleGraduationTo,
			List<String> requiredSkills,
			BigDecimal compensationMin,
			BigDecimal compensationMax,
			String compensationCurrency,
			OffsetDateTime createdAt,
			OffsetDateTime updatedAt) {
	}

	public record OpportunityPageResponse(
			List<OpportunityResponse> items,
			int page,
			int size,
			long totalItems) {
	}
}
