package com.internflow.task;

public class TaskNotFoundException extends RuntimeException {

	public TaskNotFoundException() {
		super("The requested task or linked application does not exist.");
	}
}
