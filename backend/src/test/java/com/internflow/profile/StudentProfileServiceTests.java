package com.internflow.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.internflow.profile.StudentProfileDtos.ProfileRequest;
import com.internflow.profile.StudentProfileRepository.ProfileRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudentProfileServiceTests {

	@Mock
	private StudentProfileRepository repository;

	@InjectMocks
	private StudentProfileService service;

	@Test
	void updateUsesAuthenticatedUserIdAndNormalizesValues() {
		UUID userId = UUID.randomUUID();
		ProfileRequest request = new ProfileRequest(
				"  North University ",
				"  Computer Science ",
				LocalDate.of(2027, 6, 1),
				3,
				List.of(" Software Engineering ", "Software Engineering"),
				List.of(" Java ", "SQL"),
				List.of(" Boston "),
				List.of("hybrid"));
		ProfileRecord record = new ProfileRecord(
				"North University",
				"Computer Science",
				request.graduationDate(),
				3,
				List.of("Software Engineering"),
				List.of("Java", "SQL"),
				List.of("Boston"),
				List.of("hybrid"));
		when(repository.update(eq(userId), any(ProfileRequest.class))).thenReturn(Optional.of(record));

		assertThat(service.update(userId, request).university()).isEqualTo("North University");

		ArgumentCaptor<ProfileRequest> captor = ArgumentCaptor.forClass(ProfileRequest.class);
		verify(repository).update(eq(userId), captor.capture());
		assertThat(captor.getValue().targetRoleCategories()).containsExactly("Software Engineering");
		assertThat(captor.getValue().skills()).containsExactly("Java", "SQL");
		assertThat(captor.getValue().preferredLocations()).containsExactly("Boston");
	}

	@Test
	void getIsScopedToTheAuthenticatedUserId() {
		UUID userId = UUID.randomUUID();
		when(repository.findByUserId(userId)).thenReturn(Optional.of(new ProfileRecord(
				null, null, null, null, List.of(), List.of(), List.of(), List.of())));

		assertThat(service.get(userId).skills()).isEmpty();
		verify(repository).findByUserId(userId);
	}
}
