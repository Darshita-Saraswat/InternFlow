package com.internflow.application;

public class ApplicationNotFoundException extends RuntimeException {

	public ApplicationNotFoundException() {
		super("The application was not found.");
	}
}
