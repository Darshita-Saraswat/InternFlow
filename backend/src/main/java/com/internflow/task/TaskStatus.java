package com.internflow.task;

import java.util.Locale;

public enum TaskStatus {
	TODO,
	IN_PROGRESS,
	COMPLETED;

	public String databaseValue() {
		return name().toLowerCase(Locale.ROOT);
	}
}
