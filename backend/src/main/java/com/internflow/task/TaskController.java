package com.internflow.task;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.internflow.auth.AccountPrincipal;
import com.internflow.task.TaskDtos.TaskPageResponse;
import com.internflow.task.TaskDtos.TaskRequest;
import com.internflow.task.TaskDtos.TaskResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

	private final TaskService taskService;

	public TaskController(TaskService taskService) {
		this.taskService = taskService;
	}

	@GetMapping
	TaskPageResponse list(
			@AuthenticationPrincipal AccountPrincipal principal,
			@RequestParam(required = false) TaskStatus status,
			@RequestParam(required = false) TaskPriority priority,
			@RequestParam(required = false) TaskCategory category,
			@RequestParam(required = false) UUID applicationId,
			@RequestParam(required = false) @Size(max = 200) String q,
			@RequestParam(required = false) OffsetDateTime dueFrom,
			@RequestParam(required = false) OffsetDateTime dueTo,
			@RequestParam(defaultValue = "false") boolean overdue,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			@RequestParam(defaultValue = "DUE_AT") TaskSort sort,
			@RequestParam(defaultValue = "ASC") TaskSortDirection direction) {
		return taskService.list(
				principal.id(),
				status,
				priority,
				category,
				applicationId,
				q,
				dueFrom,
				dueTo,
				overdue,
				page,
				size,
				sort,
				direction);
	}

	@PostMapping
	ResponseEntity<TaskResponse> create(
			@AuthenticationPrincipal AccountPrincipal principal,
			@Valid @RequestBody TaskRequest request) {
		TaskResponse created = taskService.create(principal.id(), request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(created.id())
				.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@GetMapping("/{taskId}")
	TaskResponse get(
			@AuthenticationPrincipal AccountPrincipal principal,
			@PathVariable UUID taskId) {
		return taskService.get(principal.id(), taskId);
	}

	@PutMapping("/{taskId}")
	TaskResponse update(
			@AuthenticationPrincipal AccountPrincipal principal,
			@PathVariable UUID taskId,
			@Valid @RequestBody TaskRequest request) {
		return taskService.update(principal.id(), taskId, request);
	}

	@DeleteMapping("/{taskId}")
	ResponseEntity<Void> delete(
			@AuthenticationPrincipal AccountPrincipal principal,
			@PathVariable UUID taskId) {
		taskService.delete(principal.id(), taskId);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}
}
