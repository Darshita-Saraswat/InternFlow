package com.internflow.task;

public class InvalidTaskFilterException extends RuntimeException {

	public InvalidTaskFilterException() {
		super("The deadline start must be before or equal to the deadline end.");
	}
}
