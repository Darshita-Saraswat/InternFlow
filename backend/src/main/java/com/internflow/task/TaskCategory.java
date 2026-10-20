package com.internflow.task;

import java.util.Locale;

public enum TaskCategory {
	RESUME,
	RESEARCH,
	ONLINE_ASSESSMENT,
	DSA_PRACTICE,
	INTERVIEW_PREPARATION,
	NETWORKING,
	FOLLOW_UP,
	DOCUMENTS,
	OTHER;

	public String databaseValue() {
		return name().toLowerCase(Locale.ROOT);
	}
}
