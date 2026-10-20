package com.internflow.task;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class TaskDtos {

	private TaskDtos() {
	}

	public record TaskRequest(
			@NotBlank @Size(max = 200) String title,
			@Size(max = 10000) String description,
			@NotNull TaskPriority priority,
			@NotNull TaskCategory category,
			@NotNull TaskStatus status,
			OffsetDateTime dueAt,
			UUID applicationId) {
	}

	public record TaskResponse(
			UUID id,
			UUID applicationId,
			String applicationCompany,
			String applicationRoleTitle,
			String title,
			String description,
			TaskPriority priority,
			TaskCategory category,
			TaskStatus status,
			OffsetDateTime dueAt,
			OffsetDateTime completedAt,
			OffsetDateTime createdAt,
			OffsetDateTime updatedAt) {
	}

	public record TaskPageResponse(List<TaskResponse> items, int page, int size, long totalItems) {
	}
}
