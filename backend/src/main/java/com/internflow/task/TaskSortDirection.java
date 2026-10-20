package com.internflow.task;

public enum TaskSortDirection {
	ASC("ASC"),
	DESC("DESC");

	private final String sql;

	TaskSortDirection(String sql) {
		this.sql = sql;
	}

	public String sql() {
		return sql;
	}
}
