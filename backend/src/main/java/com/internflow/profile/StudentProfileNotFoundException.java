package com.internflow.profile;

public class StudentProfileNotFoundException extends RuntimeException {

	public StudentProfileNotFoundException() {
		super("The requested student profile does not exist.");
	}
}
