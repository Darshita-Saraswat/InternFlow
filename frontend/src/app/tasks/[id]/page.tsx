"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { ApplicationWorkspace } from "@/components/application-workspace";
import { TaskForm } from "@/components/task-form";
import { ApiError } from "@/lib/auth-api";
import { deleteTask, getTask, updateTask } from "@/lib/tasks-api";
import type { Task } from "@/lib/tasks-api";

function labelFor(value: string) {
  return value.replaceAll("_", " ").toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function dateTimeLabel(value: string | null) {
  return value
    ? new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(value))
    : "No deadline set";
}

export default function TaskDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const router = useRouter();
  const [id, setId] = useState("");
  const [task, setTask] = useState<Task | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [editing, setEditing] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [retryCount, setRetryCount] = useState(0);

  useEffect(() => {
    void params.then(({ id: taskId }) => setId(taskId));
  }, [params]);

  useEffect(() => {
    if (!id) return;
    let current = true;
    getTask(id)
      .then((loadedTask) => { if (current) { setTask(loadedTask); setError(""); } })
      .catch((caught: unknown) => {
        if (!current || (caught instanceof ApiError && caught.status === 401)) return;
        setError(caught instanceof Error ? caught.message : "This task could not be loaded.");
      })
      .finally(() => { if (current) setLoading(false); });
    return () => { current = false; };
  }, [id, retryCount]);

  async function remove() {
    if (!task || !window.confirm(`Delete “${task.title}”? This cannot be undone.`)) return;
    setDeleting(true);
    setError("");
    try {
      await deleteTask(task.id);
      router.push("/tasks");
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "The task could not be deleted.");
      setDeleting(false);
    }
  }

  if (loading) {
    return <ApplicationWorkspace activeSection="tasks" description="Loading the selected task." title="Task details"><div className="application-state" aria-live="polite"><span className="spinner workspace-spinner" aria-hidden="true" /> Loading task…</div></ApplicationWorkspace>;
  }

  if (!task || error) {
    return (
      <ApplicationWorkspace activeSection="tasks" description="Review or update this task." title="Task details">
        <section className="application-panel application-state" role="alert">
          <h2>{task ? "Task action failed" : "Couldn’t open this task"}</h2>
          <p>{error || "The task could not be found."}</p>
          {task ? <button className="button button-outline" onClick={() => setError("")} type="button">Continue editing</button> : <button className="button button-outline" onClick={() => { setLoading(true); setRetryCount((count) => count + 1); }} type="button">Try again</button>}
          <Link className="text-link" href="/tasks">Back to tasks</Link>
        </section>
      </ApplicationWorkspace>
    );
  }

  return (
    <ApplicationWorkspace
      activeSection="tasks"
      actions={!editing ? <>
        <button className="button button-outline" onClick={() => setEditing(true)} type="button">Edit task</button>
        <button className="button button-danger" disabled={deleting} onClick={() => void remove()} type="button">{deleting ? "Deleting…" : "Delete"}</button>
      </> : undefined}
      description={editing ? "Update the task details and status." : "Review the deadline, status, and linked opportunity."}
      title={editing ? "Edit task" : task.title}
    >
      {error && <p className="workspace-toast" role="alert">{error}</p>}
      {editing ? (
        <section className="application-panel">
          <div className="panel-intro"><Link className="text-link" href="/tasks">← Back to tasks</Link></div>
          <TaskForm
            initial={task}
            onCancel={() => setEditing(false)}
            onSubmit={async (input) => {
              const updated = await updateTask(task.id, input);
              setTask(updated);
              setEditing(false);
            }}
            submitLabel="Save changes"
          />
        </section>
      ) : (
        <section aria-label="Task details" className="application-panel">
          <div className="detail-summary">
            <div>
              <span className={`status-pill status-${task.status.toLowerCase()}`}>{labelFor(task.status)}</span>
              <span className={`priority-pill priority-${task.priority.toLowerCase()}`}>{labelFor(task.priority)} priority</span>
              <p>{labelFor(task.category)}{task.applicationCompany ? ` · ${task.applicationCompany} — ${task.applicationRoleTitle}` : ""}</p>
            </div>
            <aside><span>Deadline</span><strong>{dateTimeLabel(task.dueAt)}</strong></aside>
          </div>
          <dl className="detail-grid">
            <div><dt>Created</dt><dd>{dateTimeLabel(task.createdAt)}</dd></div>
            <div><dt>Last updated</dt><dd>{dateTimeLabel(task.updatedAt)}</dd></div>
            <div><dt>Completed</dt><dd>{task.completedAt ? dateTimeLabel(task.completedAt) : "Not completed"}</dd></div>
            {task.description && <div className="detail-wide"><dt>Description</dt><dd className="detail-notes">{task.description}</dd></div>}
          </dl>
          <div className="activity-section"><Link className="text-link" href="/tasks">← Back to tasks</Link></div>
        </section>
      )}
    </ApplicationWorkspace>
  );
}
