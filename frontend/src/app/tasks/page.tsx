"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { ApplicationWorkspace } from "@/components/application-workspace";
import { ApiError } from "@/lib/auth-api";
import { getTasks, updateTask } from "@/lib/tasks-api";
import type { Task, TaskCategory, TaskPage, TaskPriority, TaskSort, TaskStatus } from "@/lib/tasks-api";

const statuses: Array<TaskStatus | ""> = ["", "TODO", "IN_PROGRESS", "COMPLETED"];
const categories: Array<TaskCategory | ""> = [
  "", "RESUME", "RESEARCH", "ONLINE_ASSESSMENT", "DSA_PRACTICE", "INTERVIEW_PREPARATION",
  "NETWORKING", "FOLLOW_UP", "DOCUMENTS", "OTHER",
];
const priorities: Array<TaskPriority | ""> = ["", "LOW", "NORMAL", "HIGH"];

function labelFor(value: string) {
  return value
    ? value.replaceAll("_", " ").toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase()).replace(/\bDsa\b/g, "DSA")
    : "All";
}

function endOfLocalDay(value: string) {
  return new Date(`${value}T23:59:59.999`)
    .toISOString()
    .replace(".999Z", ".999999Z");
}

function deadlineLabel(value: string | null) {
  if (!value) return "No deadline";
  return new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}

function isOverdue(task: Task) {
  return task.status !== "COMPLETED" && task.dueAt !== null && new Date(task.dueAt).getTime() < Date.now();
}

export default function TasksPage() {
  const [result, setResult] = useState<TaskPage>({ items: [], page: 0, size: 20, totalItems: 0 });
  const [queryInput, setQueryInput] = useState("");
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState<TaskStatus | "">("");
  const [priority, setPriority] = useState<TaskPriority | "">("");
  const [category, setCategory] = useState<TaskCategory | "">("");
  const [overdue, setOverdue] = useState(false);
  const [dueFrom, setDueFrom] = useState("");
  const [dueTo, setDueTo] = useState("");
  const [sort, setSort] = useState<TaskSort>("DUE_AT");
  const [direction, setDirection] = useState<"ASC" | "DESC">("ASC");
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [actionError, setActionError] = useState("");
  const [retryCount, setRetryCount] = useState(0);

  useEffect(() => {
    let current = true;
    getTasks({
      query,
      status,
      priority,
      category,
      overdue,
      dueFrom: dueFrom ? new Date(`${dueFrom}T00:00:00`).toISOString() : undefined,
      dueTo: dueTo ? endOfLocalDay(dueTo) : undefined,
      page,
      size: 20,
      sort,
      direction,
    })
      .then((tasks) => {
        if (!current) return;
        setResult(tasks);
        setError("");
      })
      .catch((caught: unknown) => {
        if (!current || (caught instanceof ApiError && caught.status === 401)) return;
        setError(caught instanceof Error ? caught.message : "Tasks could not be loaded.");
      })
      .finally(() => { if (current) setLoading(false); });
    return () => { current = false; };
  }, [query, status, priority, category, overdue, dueFrom, dueTo, page, sort, direction, retryCount]);

  function search(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setPage(0);
    setQuery(queryInput);
  }

  function clearFilters() {
    setLoading(true);
    setQuery("");
    setQueryInput("");
    setStatus("");
    setPriority("");
    setCategory("");
    setOverdue(false);
    setDueFrom("");
    setDueTo("");
    setPage(0);
  }

  async function toggleCompletion(task: Task) {
    setActionError("");
    try {
      await updateTask(task.id, {
        title: task.title,
        description: task.description,
        priority: task.priority,
        category: task.category,
        status: task.status === "COMPLETED" ? "TODO" : "COMPLETED",
        dueAt: task.dueAt,
        applicationId: task.applicationId,
      });
      setLoading(true);
      setRetryCount((count) => count + 1);
    } catch (caught) {
      setActionError(caught instanceof Error ? caught.message : "The task status could not be updated.");
    }
  }

  const lastPage = Math.max(0, Math.ceil(result.totalItems / 20) - 1);
  const hasFilters = Boolean(query || status || priority || category || overdue || dueFrom || dueTo);

  return (
    <ApplicationWorkspace
      activeSection="tasks"
      actions={<Link className="button" href="/tasks/new">Add task <span aria-hidden="true">+</span></Link>}
      description="Turn deadlines and preparation into clear next steps."
      title="Tasks"
    >
      <section aria-label="Task records" className="application-panel">
        <form className="application-filters task-filters" onSubmit={search}>
          <label className="field search-field"><span>Search tasks</span><input maxLength={200} onChange={(event) => setQueryInput(event.target.value)} placeholder="Title or description" value={queryInput} /></label>
          <label className="field"><span>Status</span><select onChange={(event) => { setLoading(true); setPage(0); setStatus(event.target.value as TaskStatus | ""); }} value={status}>{statuses.map((value) => <option key={value || "all"} value={value}>{labelFor(value)}{value ? "" : " statuses"}</option>)}</select></label>
          <label className="field"><span>Category</span><select onChange={(event) => { setLoading(true); setPage(0); setCategory(event.target.value as TaskCategory | ""); }} value={category}>{categories.map((value) => <option key={value || "all"} value={value}>{labelFor(value)}{value ? "" : " categories"}</option>)}</select></label>
          <label className="field"><span>Priority</span><select onChange={(event) => { setLoading(true); setPage(0); setPriority(event.target.value as TaskPriority | ""); }} value={priority}>{priorities.map((value) => <option key={value || "all"} value={value}>{labelFor(value)}{value ? "" : " priorities"}</option>)}</select></label>
          <label className="field"><span>Sort by</span><select onChange={(event) => { setLoading(true); setPage(0); setSort(event.target.value as TaskSort); }} value={sort}><option value="DUE_AT">Deadline</option><option value="CREATED_AT">Created date</option><option value="UPDATED_AT">Last updated</option></select></label>
          <label className="field"><span>Deadline from</span><input onChange={(event) => { setLoading(true); setPage(0); setDueFrom(event.target.value); }} type="date" value={dueFrom} /></label>
          <label className="field"><span>Deadline through</span><input onChange={(event) => { setLoading(true); setPage(0); setDueTo(event.target.value); }} type="date" value={dueTo} /></label>
          <button className="button" type="submit">Search</button>
          <label className="task-overdue-filter"><input checked={overdue} onChange={(event) => { setLoading(true); setPage(0); setOverdue(event.target.checked); }} type="checkbox" /> Overdue only</label>
          <label className="task-overdue-filter"><input checked={direction === "DESC"} onChange={(event) => { setLoading(true); setDirection(event.target.checked ? "DESC" : "ASC"); }} type="checkbox" /> Latest first</label>
          {hasFilters && <button className="button button-outline" onClick={clearFilters} type="button">Clear filters</button>}
        </form>

        {actionError && <p className="workspace-toast" role="alert">{actionError}</p>}
        {loading ? (
          <div className="application-state" aria-live="polite"><span className="spinner workspace-spinner" aria-hidden="true" /> Loading your tasks…</div>
        ) : error ? (
          <div className="application-state" role="alert"><h2>Couldn’t load tasks</h2><p>{error}</p><button className="button button-outline" onClick={() => { setLoading(true); setRetryCount((count) => count + 1); }} type="button">Try again</button></div>
        ) : result.items.length === 0 ? (
          <div className="application-state">
            <span aria-hidden="true" className="empty-mark">✓</span>
            <h2>{hasFilters ? "No tasks match these filters." : "Your task list is ready."}</h2>
            <p>{hasFilters ? "Try different filters or clear them to see all tasks." : "Add a deadline, preparation step, or follow-up to keep your search moving."}</p>
            {hasFilters ? <button className="button button-outline" onClick={clearFilters} type="button">Clear filters</button> : <Link className="button" href="/tasks/new">Add your first task</Link>}
          </div>
        ) : (
          <>
            <ul className="application-list task-list">
              {result.items.map((task) => (
                <li key={task.id}>
                  <div className="task-row">
                    <button
                      aria-label={task.status === "COMPLETED" ? `Mark ${task.title} as not complete` : `Mark ${task.title} as complete`}
                      className={`task-complete ${task.status === "COMPLETED" ? "task-complete-done" : ""}`}
                      onClick={() => void toggleCompletion(task)}
                      type="button"
                    >{task.status === "COMPLETED" ? "✓" : ""}</button>
                    <Link className="application-card task-card" href={`/tasks/${task.id}`}>
                      <span className="application-card-main">
                        <span className="application-company">{task.title}</span>
                        <span className="application-role">{task.applicationCompany ? `${task.applicationCompany} · ${task.applicationRoleTitle}` : labelFor(task.category)}</span>
                        <span className="application-meta">{labelFor(task.category)} · {labelFor(task.priority)} priority</span>
                      </span>
                      <span className={`status-pill status-${task.status.toLowerCase()}`}>{labelFor(task.status)}</span>
                      <span className={`application-date ${isOverdue(task) ? "task-deadline-overdue" : ""}`}><span>Deadline</span>{deadlineLabel(task.dueAt)}{isOverdue(task) ? <strong>Overdue</strong> : null}</span>
                      <span aria-hidden="true" className="application-arrow">→</span>
                    </Link>
                  </div>
                </li>
              ))}
            </ul>
            <div className="pagination-row">
              <span>{result.totalItems} {result.totalItems === 1 ? "task" : "tasks"}</span>
              <div>
                <button className="button button-outline" disabled={page === 0 || loading} onClick={() => { setLoading(true); setPage((value) => value - 1); }} type="button">Previous</button>
                <span>Page {page + 1} of {lastPage + 1}</span>
                <button className="button button-outline" disabled={page >= lastPage || loading} onClick={() => { setLoading(true); setPage((value) => value + 1); }} type="button">Next</button>
              </div>
            </div>
          </>
        )}
      </section>
    </ApplicationWorkspace>
  );
}
