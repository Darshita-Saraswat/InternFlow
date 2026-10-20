package com.internflow.profile;

import java.util.List;
import java.util.UUID;

import com.internflow.profile.StudentProfileDtos.ProfileRequest;
import com.internflow.profile.StudentProfileDtos.ProfileResponse;
import com.internflow.profile.StudentProfileRepository.ProfileRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentProfileService {

	private final StudentProfileRepository repository;

	public StudentProfileService(StudentProfileRepository repository) {
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public ProfileResponse get(UUID userId) {
		return repository.findByUserId(userId)
				.map(StudentProfileService::toResponse)
				.orElseThrow(StudentProfileNotFoundException::new);
	}

	@Transactional
	public ProfileResponse update(UUID userId, ProfileRequest request) {
		ProfileRequest normalized = new ProfileRequest(
				trimToNull(request.university()),
				trimToNull(request.degree()),
				request.graduationDate(),
				request.studyYear(),
				normalizeList(request.targetRoleCategories()),
				normalizeList(request.skills()),
				normalizeList(request.preferredLocations()),
				normalizeList(request.workModes()));
		return repository.update(userId, normalized)
				.map(StudentProfileService::toResponse)
				.orElseThrow(StudentProfileNotFoundException::new);
	}

	private static List<String> normalizeList(List<String> values) {
		return values.stream().map(String::trim).distinct().toList();
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static ProfileResponse toResponse(ProfileRecord profile) {
		return new ProfileResponse(
				profile.university(),
				profile.degree(),
				profile.graduationDate(),
				profile.studyYear(),
				profile.targetRoleCategories(),
				profile.skills(),
				profile.preferredLocations(),
				profile.workModes());
	}
}
