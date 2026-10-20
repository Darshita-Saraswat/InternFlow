package com.internflow.common;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import com.internflow.auth.EmailAlreadyRegisteredException;
import com.internflow.auth.InvalidCredentialsException;
import com.internflow.application.ApplicationNotFoundException;
import com.internflow.opportunity.OpportunityNotFoundException;
import com.internflow.profile.StudentProfileNotFoundException;
import com.internflow.task.InvalidTaskFilterException;
import com.internflow.task.TaskNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(EmailAlreadyRegisteredException.class)
	ResponseEntity<ProblemDetail> handleDuplicateAccount(EmailAlreadyRegisteredException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
		problem.setTitle("Account already exists");
		return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	ResponseEntity<ProblemDetail> handleInvalidCredentials(InvalidCredentialsException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
		problem.setTitle("Invalid credentials");
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
	}

	@ExceptionHandler(ApplicationNotFoundException.class)
	ResponseEntity<ProblemDetail> handleApplicationNotFound() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
				HttpStatus.NOT_FOUND, "The requested application does not exist.");
		problem.setTitle("Application not found");
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
	}

	@ExceptionHandler(OpportunityNotFoundException.class)
	ResponseEntity<ProblemDetail> handleOpportunityNotFound() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
				HttpStatus.NOT_FOUND, "The requested catalogue opportunity does not exist.");
		problem.setTitle("Opportunity not found");
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
	}

	@ExceptionHandler(StudentProfileNotFoundException.class)
	ResponseEntity<ProblemDetail> handleStudentProfileNotFound() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
				HttpStatus.NOT_FOUND, "The requested student profile does not exist.");
		problem.setTitle("Student profile not found");
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
	}

	@ExceptionHandler(TaskNotFoundException.class)
	ResponseEntity<ProblemDetail> handleTaskNotFound() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
				HttpStatus.NOT_FOUND, "The requested task or linked application does not exist.");
		problem.setTitle("Task not found");
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
	}

	@ExceptionHandler(InvalidTaskFilterException.class)
	ResponseEntity<ProblemDetail> handleInvalidTaskFilter(InvalidTaskFilterException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
		problem.setTitle("Invalid task filters");
		return ResponseEntity.badRequest().body(problem);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException exception) {
		Map<String, ArrayList<String>> fieldErrors = new TreeMap<>();
		exception.getBindingResult().getFieldErrors().forEach(error -> fieldErrors
				.computeIfAbsent(error.getField(), ignored -> new ArrayList<>())
				.add(error.getDefaultMessage()));
		fieldErrors.values().forEach(messages -> messages.sort(String::compareTo));

		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
				HttpStatus.BAD_REQUEST, "One or more request fields are invalid.");
		problem.setTitle("Request validation failed");
		problem.setProperty("fieldErrors", fieldErrors);
		return ResponseEntity.badRequest().body(problem);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ProblemDetail> handleUnreadableBody() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
				HttpStatus.BAD_REQUEST, "The request body is missing or malformed.");
		problem.setTitle("Invalid request body");
		return ResponseEntity.badRequest().body(problem);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	ResponseEntity<ProblemDetail> handleMethodValidation() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
				HttpStatus.BAD_REQUEST, "One or more request parameters are invalid.");
		problem.setTitle("Request validation failed");
		return ResponseEntity.badRequest().body(problem);
	}
}
