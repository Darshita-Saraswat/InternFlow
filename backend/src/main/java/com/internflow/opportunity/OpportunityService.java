package com.internflow.opportunity;

import java.util.List;
import java.util.UUID;

import com.internflow.opportunity.OpportunityDtos.OpportunityPageResponse;
import com.internflow.opportunity.OpportunityDtos.OpportunityResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpportunityService {

	private final OpportunityRepository repository;

	public OpportunityService(OpportunityRepository repository) {
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public OpportunityPageResponse list(
			String query,
			String roleType,
			String location,
			String workMode,
			int page,
			int size) {
		String normalizedQuery = trimToNull(query);
		String normalizedRoleType = trimToNull(roleType);
		String normalizedLocation = trimToNull(location);
		String normalizedWorkMode = trimToNull(workMode);
		List<OpportunityResponse> items = repository.findAll(
				normalizedQuery, normalizedRoleType, normalizedLocation, normalizedWorkMode, page, size);
		long totalItems = repository.count(
				normalizedQuery, normalizedRoleType, normalizedLocation, normalizedWorkMode);
		return new OpportunityPageResponse(items, page, size, totalItems);
	}

	@Transactional(readOnly = true)
	public OpportunityResponse get(UUID opportunityId) {
		return repository.findById(opportunityId).orElseThrow(OpportunityNotFoundException::new);
	}

	private static String trimToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
