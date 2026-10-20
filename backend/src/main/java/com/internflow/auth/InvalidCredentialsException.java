package com.internflow.auth;

public class InvalidCredentialsException extends RuntimeException {

	public InvalidCredentialsException() {
		super("Email or password is incorrect.");
	}
}
