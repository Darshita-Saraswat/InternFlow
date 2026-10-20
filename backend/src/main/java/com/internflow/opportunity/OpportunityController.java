package com.internflow.opportunity;

import java.util.UUID;

import com.internflow.auth.AccountPrincipal;
import com.internflow.opportunity.FitAssessmentDtos.FitAssessmentResponse;
import com.internflow.opportunity.OpportunityDtos.OpportunityPageResponse;
import com.internflow.opportunity.OpportunityDtos.OpportunityResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/opportunities")
public class OpportunityController {

	private final OpportunityService service;
	private final OpportunityFitAssessmentService fitAssessmentService;

	public OpportunityController(
			OpportunityService service,
			OpportunityFitAssessmentService fitAssessmentService) {
		this.service = service;
		this.fitAssessmentService = fitAssessmentService;
	}

	@GetMapping
	OpportunityPageResponse list(
			@RequestParam(required = false) @Size(max = 200) String q,
			@RequestParam(required = false)
			@Pattern(regexp = "|internship|graduate_role|analyst_role|early_career_other") String roleType,
			@RequestParam(required = false) @Size(max = 200) String location,
			@RequestParam(required = false) @Pattern(regexp = "|in_person|hybrid|remote") String workMode,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return service.list(q, roleType, location, workMode, page, size);
	}

	@GetMapping("/{opportunityId}")
	OpportunityResponse get(@PathVariable UUID opportunityId) {
		return service.get(opportunityId);
	}

	@GetMapping("/{opportunityId}/fit-assessment")
	FitAssessmentResponse assessFit(
			@AuthenticationPrincipal AccountPrincipal principal,
			@PathVariable UUID opportunityId) {
		return fitAssessmentService.assess(principal.id(), opportunityId);
	}
}
