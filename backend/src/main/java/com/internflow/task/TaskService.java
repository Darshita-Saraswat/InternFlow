package com.internflow.task;

import java.util.List;
import java.util.UUID;

import com.internflow.task.TaskDtos.TaskPageResponse;
import com.internflow.task.TaskDtos.TaskRequest;
import com.internflow.task.TaskDtos.TaskResponse;
import com.internflow.task.TaskRepository.TaskFilters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

	private final TaskRepository taskRepository;

	public TaskService(TaskRepository taskRepository) {
		this.taskRepository = taskRepository;
	}

	@Transactional(readOnly = true)
	public TaskPageResponse list(
			UUID userId,
			TaskStatus status,
			TaskPriority priority,
			TaskCategory category,
			UUID applicationId,
			String query,
			java.time.OffsetDateTime dueFrom,
			java.time.OffsetDateTime dueTo,
			boolean overdue,
			int page,
			int size,
			TaskSort sort,
			TaskSortDirection direction) {
		if (dueFrom != null && dueTo != null && dueFrom.isAfter(dueTo)) {
			throw new InvalidTaskFilterException();
		}
		TaskFilters filters = new TaskFilters(
				userId,
				status,
				priority,
				category,
				applicationId,
				query == null || query.isBlank() ? null : query.trim(),
				dueFrom,
				dueTo,
				overdue,
				page,
				size,
				sort,
				direction);
		List<TaskResponse> items = taskRepository.findAllOwned(filters);
		return new TaskPageResponse(items, page, size, taskRepository.countOwned(filters));
	}

	@Transactional(readOnly = true)
	public TaskResponse get(UUID userId, UUID taskId) {
		return taskRepository.findOwned(userId, taskId).orElseThrow(TaskNotFoundException::new);
	}

	@Transactional
	public TaskResponse create(UUID userId, TaskRequest request) {
		validateApplicationOwnership(userId, request.applicationId());
		return taskRepository.createOwned(userId, request);
	}

	@Transactional
	public TaskResponse update(UUID userId, UUID taskId, TaskRequest request) {
		validateApplicationOwnership(userId, request.applicationId());
		if (!taskRepository.updateOwned(userId, taskId, request)) {
			throw new TaskNotFoundException();
		}
		return taskRepository.findOwned(userId, taskId).orElseThrow(TaskNotFoundException::new);
	}

	@Transactional
	public void delete(UUID userId, UUID taskId) {
		if (!taskRepository.deleteOwned(userId, taskId)) {
			throw new TaskNotFoundException();
		}
	}

	private void validateApplicationOwnership(UUID userId, UUID applicationId) {
		if (applicationId != null && !taskRepository.applicationOwned(userId, applicationId)) {
			throw new TaskNotFoundException();
		}
	}
}
