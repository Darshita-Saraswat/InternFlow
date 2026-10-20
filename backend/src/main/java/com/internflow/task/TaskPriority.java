package com.internflow.task;

import java.util.Locale;

public enum TaskPriority {
	LOW,
	NORMAL,
	HIGH;

	public String databaseValue() {
		return name().toLowerCase(Locale.ROOT);
	}
}
