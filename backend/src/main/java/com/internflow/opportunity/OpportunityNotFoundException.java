package com.internflow.opportunity;

public class OpportunityNotFoundException extends RuntimeException {

	public OpportunityNotFoundException() {
		super("The requested catalogue opportunity does not exist.");
	}
}
