package com.internflow.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.internflow.task.TaskDtos.TaskRequest;
import com.internflow.task.TaskDtos.TaskResponse;
import com.internflow.task.TaskRepository.TaskFilters;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

class TaskServiceTests {

	private final TaskRepository repository = mock(TaskRepository.class);
	private final TaskService service = new TaskService(repository);
	private final UUID userId = UUID.randomUUID();
	private final UUID taskId = UUID.randomUUID();
	private final UUID applicationId = UUID.randomUUID();

	@Test
	void listTrimsSearchAndCombinesScopedFiltersAndCount() {
		OffsetDateTime dueFrom = OffsetDateTime.now();
		OffsetDateTime dueTo = dueFrom.plusDays(3);
		TaskFilters filters = new TaskFilters(
				userId, TaskStatus.TODO, TaskPriority.HIGH, TaskCategory.RESEARCH,
				applicationId, "resume", dueFrom, dueTo, false, 1, 10,
				TaskSort.DUE_AT, TaskSortDirection.ASC);
		TaskResponse task = task(TaskStatus.TODO);
		when(repository.findAllOwned(filters)).thenReturn(List.of(task));
		when(repository.countOwned(filters)).thenReturn(11L);

		var result = service.list(
				userId,
				TaskStatus.TODO,
				TaskPriority.HIGH,
				TaskCategory.RESEARCH,
				applicationId,
				"  resume  ",
				dueFrom,
				dueTo,
				false,
				1,
				10,
				TaskSort.DUE_AT,
				TaskSortDirection.ASC);

		assertThat(result.items()).containsExactly(task);
		assertThat(result.totalItems()).isEqualTo(11);
		assertThat(result.page()).isEqualTo(1);
		verify(repository).findAllOwned(filters);
		verify(repository).countOwned(filters);
	}

	@Test
	void listNormalizesBlankSearchAndRejectsInvertedDeadlineRange() {
		when(repository.findAllOwned(org.mockito.ArgumentMatchers.any(TaskFilters.class))).thenReturn(List.of());
		when(repository.countOwned(org.mockito.ArgumentMatchers.any(TaskFilters.class))).thenReturn(0L);
		service.list(userId, null, null, null, null, "  ", null, null, true, 0, 20,
				TaskSort.CREATED_AT, TaskSortDirection.DESC);
		org.mockito.ArgumentCaptor<TaskFilters> filters =
				org.mockito.ArgumentCaptor.forClass(TaskFilters.class);
		verify(repository).findAllOwned(filters.capture());
		assertThat(filters.getValue().query()).isNull();
		assertThat(filters.getValue().overdue()).isTrue();

		assertThatThrownBy(() -> service.list(
				userId, null, null, null, null, null,
				OffsetDateTime.now().plusDays(1), OffsetDateTime.now(),
				false, 0, 20, TaskSort.DUE_AT, TaskSortDirection.ASC))
				.isInstanceOf(InvalidTaskFilterException.class);
	}

	@Test
	void getReturnsOnlyRepositoryRecordScopedToTheCurrentUser() {
		TaskResponse task = task(TaskStatus.IN_PROGRESS);
		when(repository.findOwned(userId, taskId)).thenReturn(Optional.of(task));

		assertThat(service.get(userId, taskId)).isEqualTo(task);

		when(repository.findOwned(userId, taskId)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> service.get(userId, taskId))
				.isInstanceOf(TaskNotFoundException.class);
	}

	@Test
	void createChecksLinkedApplicationOwnershipBeforePersisting() {
		TaskRequest request = request(TaskStatus.TODO, applicationId);
		when(repository.applicationOwned(userId, applicationId)).thenReturn(true);
		TaskResponse saved = task(TaskStatus.TODO);
		when(repository.createOwned(userId, request)).thenReturn(saved);

		assertThat(service.create(userId, request)).isEqualTo(saved);
		verify(repository).createOwned(userId, request);

		org.mockito.Mockito.clearInvocations(repository);
		when(repository.applicationOwned(userId, applicationId)).thenReturn(false);
		assertThatThrownBy(() -> service.create(userId, request))
				.isInstanceOf(TaskNotFoundException.class);
		verify(repository, never()).createOwned(userId, request);
	}

	@Test
	void updateChecksApplicationOwnershipAndTaskOwnership() {
		TaskRequest request = request(TaskStatus.COMPLETED, null);
		TaskResponse saved = task(TaskStatus.COMPLETED);
		when(repository.updateOwned(userId, taskId, request)).thenReturn(true);
		when(repository.findOwned(userId, taskId)).thenReturn(Optional.of(saved));

		assertThat(service.update(userId, taskId, request)).isEqualTo(saved);
		verify(repository).updateOwned(userId, taskId, request);

		when(repository.updateOwned(userId, taskId, request)).thenReturn(false);
		assertThatThrownBy(() -> service.update(userId, taskId, request))
				.isInstanceOf(TaskNotFoundException.class);
	}

	@Test
	void deleteRequiresOwnedTask() {
		when(repository.deleteOwned(userId, taskId)).thenReturn(false);
		assertThatThrownBy(() -> service.delete(userId, taskId))
				.isInstanceOf(TaskNotFoundException.class);
		when(repository.deleteOwned(userId, taskId)).thenReturn(true);
		service.delete(userId, taskId);
		verify(repository, org.mockito.Mockito.times(2)).deleteOwned(userId, taskId);
	}

	@Test
	void requestValidationRejectsMissingAndOversizedFields() {
		TaskRequest invalid = new TaskRequest(
				" ", "x".repeat(10001), null, null, null, null, null);
		try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
			Validator validator = factory.getValidator();
			Set<String> fields = validator.validate(invalid).stream()
					.map(violation -> violation.getPropertyPath().toString())
					.collect(Collectors.toSet());
			assertThat(fields).contains("title", "description", "priority", "category", "status");
		}
	}

	@Test
	void requestValidationAcceptsValidTask() {
		try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
			assertThat(factory.getValidator().validate(request(TaskStatus.TODO, null))).isEmpty();
		}
	}

	private TaskRequest request(TaskStatus status, UUID linkedApplicationId) {
		return new TaskRequest(
				"Prepare interview notes",
				"Review the role and prepare examples.",
				TaskPriority.NORMAL,
				TaskCategory.INTERVIEW_PREPARATION,
				status,
				OffsetDateTime.now().plusDays(2),
				linkedApplicationId);
	}

	private TaskResponse task(TaskStatus status) {
		OffsetDateTime now = OffsetDateTime.now();
		return new TaskResponse(
				taskId,
				applicationId,
				"Northstar Labs",
				"Software Intern",
				"Prepare interview notes",
				"Review the role and prepare examples.",
				TaskPriority.NORMAL,
				TaskCategory.INTERVIEW_PREPARATION,
				status,
				now.plusDays(2),
				status == TaskStatus.COMPLETED ? now : null,
				now,
				now);
	}
}
