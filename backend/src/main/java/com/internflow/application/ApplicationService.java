package com.internflow.application;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.internflow.application.ApplicationDtos.ApplicationDetailResponse;
import com.internflow.application.ApplicationDtos.ApplicationPageResponse;
import com.internflow.application.ApplicationDtos.ApplicationRequest;
import com.internflow.application.ApplicationDtos.ApplicationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationService {

	private final ApplicationRepository applicationRepository;

	public ApplicationService(ApplicationRepository applicationRepository) {
		this.applicationRepository = applicationRepository;
	}

	@Transactional(readOnly = true)
	public ApplicationPageResponse list(
			UUID userId,
			ApplicationStatus status,
			String query,
			int page,
			int size) {
		String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
		return new ApplicationPageResponse(
				applicationRepository.findAllOwned(userId, status, normalizedQuery, page, size),
				page,
				size,
				applicationRepository.countOwned(userId, status, normalizedQuery));
	}

	@Transactional(readOnly = true)
	public ApplicationDetailResponse get(UUID userId, UUID applicationId) {
		ApplicationResponse application = findOwned(userId, applicationId);
		return new ApplicationDetailResponse(
				application,
				applicationRepository.findEventsOwned(userId, applicationId));
	}

	@Transactional
	public ApplicationResponse create(UUID userId, ApplicationRequest request) {
		ApplicationResponse application = applicationRepository.createOwned(userId, request);
		applicationRepository.insertEvent(
				userId,
				application.id(),
				"created",
				null,
				application.status(),
				"Application added to your tracker.");
		return application;
	}

	@Transactional
	public ApplicationResponse update(UUID userId, UUID applicationId, ApplicationRequest request) {
		ApplicationResponse existing = findOwned(userId, applicationId);
		if (!applicationRepository.updateOwned(userId, applicationId, request)) {
			throw new ApplicationNotFoundException();
		}
		boolean statusChanged = existing.status() != request.status();
		applicationRepository.insertEvent(
				userId,
				applicationId,
				statusChanged ? "status_changed" : "updated",
				statusChanged ? existing.status() : null,
				request.status(),
				statusChanged
						? "Status changed to "
								+ request.status().name().replace('_', ' ').toLowerCase(Locale.ROOT) + "."
						: "Application details updated.");
		return findOwned(userId, applicationId);
	}

	@Transactional
	public void delete(UUID userId, UUID applicationId) {
		if (!applicationRepository.deleteOwned(userId, applicationId)) {
			throw new ApplicationNotFoundException();
		}
	}

	private ApplicationResponse findOwned(UUID userId, UUID applicationId) {
		return applicationRepository.findDetailsOwned(userId, applicationId)
				.orElseThrow(ApplicationNotFoundException::new);
	}
}
