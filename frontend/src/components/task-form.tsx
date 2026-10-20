"use client";

import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { getApplications } from "@/lib/applications-api";
import type { Application } from "@/lib/applications-api";
import type { Task, TaskCategory, TaskInput, TaskPriority, TaskStatus } from "@/lib/tasks-api";

type Draft = {
  title: string;
  description: string;
  priority: TaskPriority;
  category: TaskCategory;
  status: TaskStatus;
  dueAt: string;
  applicationId: string;
};

const categories: TaskCategory[] = [
  "RESUME", "RESEARCH", "ONLINE_ASSESSMENT", "DSA_PRACTICE", "INTERVIEW_PREPARATION",
  "NETWORKING", "FOLLOW_UP", "DOCUMENTS", "OTHER",
];
const priorities: TaskPriority[] = ["LOW", "NORMAL", "HIGH"];
const statuses: TaskStatus[] = ["TODO", "IN_PROGRESS", "COMPLETED"];

function labelFor(value: string) {
  return value
    .replaceAll("_", " ")
    .toLowerCase()
    .replace(/\b\w/g, (letter) => letter.toUpperCase())
    .replace(/\bDsa\b/g, "DSA");
}

function localDateTime(value: string | null) {
  if (!value) return "";
  const date = new Date(value);
  const part = (number: number) => String(number).padStart(2, "0");
  return `${date.getFullYear()}-${part(date.getMonth() + 1)}-${part(date.getDate())}T${part(date.getHours())}:${part(date.getMinutes())}`;
}

function draftFor(task?: Task): Draft {
  return {
    title: task?.title ?? "",
    description: task?.description ?? "",
    priority: task?.priority ?? "NORMAL",
    category: task?.category ?? "OTHER",
    status: task?.status ?? "TODO",
    dueAt: localDateTime(task?.dueAt ?? null),
    applicationId: task?.applicationId ?? "",
  };
}

function inputFor(draft: Draft): TaskInput {
  return {
    title: draft.title.trim(),
    description: draft.description.trim() || null,
    priority: draft.priority,
    category: draft.category,
    status: draft.status,
    dueAt: draft.dueAt ? new Date(draft.dueAt).toISOString() : null,
    applicationId: draft.applicationId || null,
  };
}

export function TaskForm({
  initial,
  submitLabel,
  onSubmit,
  onCancel,
}: {
  initial?: Task;
  submitLabel: string;
  onSubmit: (input: TaskInput) => Promise<void>;
  onCancel?: () => void;
}) {
  const [draft, setDraft] = useState<Draft>(() => draftFor(initial));
  const [applications, setApplications] = useState<Application[]>([]);
  const [applicationsLoading, setApplicationsLoading] = useState(true);
  const [applicationsError, setApplicationsError] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let current = true;
    async function loadApplications() {
      const allApplications: Application[] = [];
      let page = 0;
      let total = 0;
      do {
        const result = await getApplications({ page, size: 100 });
        allApplications.push(...result.items);
        total = result.totalItems;
        page += 1;
      } while (allApplications.length < total);
      return allApplications;
    }
    loadApplications()
      .then((allApplications) => { if (current) setApplications(allApplications); })
      .catch((caught: unknown) => {
        if (current) setApplicationsError(caught instanceof Error ? caught.message : "Your applications could not be loaded.");
      })
      .finally(() => { if (current) setApplicationsLoading(false); });
    return () => { current = false; };
  }, []);

  function update<K extends keyof Draft>(key: K, value: Draft[K]) {
    setDraft((current) => ({ ...current, [key]: value }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      await onSubmit(inputFor(draft));
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Your task could not be saved.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="application-form" onSubmit={handleSubmit}>
      <section className="form-section">
        <h2>Task details</h2>
        <div className="application-fields">
          <label className="field field-wide"><span>Task title *</span><input autoComplete="off" maxLength={200} onChange={(event) => update("title", event.target.value)} required value={draft.title} /></label>
          <label className="field"><span>Category *</span><select onChange={(event) => update("category", event.target.value as TaskCategory)} value={draft.category}>{categories.map((value) => <option key={value} value={value}>{labelFor(value)}</option>)}</select></label>
          <label className="field"><span>Priority *</span><select onChange={(event) => update("priority", event.target.value as TaskPriority)} value={draft.priority}>{priorities.map((value) => <option key={value} value={value}>{labelFor(value)}</option>)}</select></label>
          <label className="field"><span>Status *</span><select onChange={(event) => update("status", event.target.value as TaskStatus)} value={draft.status}>{statuses.map((value) => <option key={value} value={value}>{labelFor(value)}</option>)}</select></label>
          <label className="field"><span>Deadline</span><input onChange={(event) => update("dueAt", event.target.value)} type="datetime-local" value={draft.dueAt} /></label>
          <label className="field">
            <span>Related application</span>
            <select disabled={applicationsLoading || Boolean(applicationsError)} onChange={(event) => update("applicationId", event.target.value)} value={draft.applicationId}>
              <option value="">{applicationsLoading ? "Loading applications…" : "No linked application"}</option>
              {applications.map((application) => <option key={application.id} value={application.id}>{application.company} — {application.roleTitle}</option>)}
            </select>
          </label>
          <label className="field field-wide"><span>Description</span><textarea maxLength={10000} onChange={(event) => update("description", event.target.value)} rows={5} value={draft.description} /></label>
        </div>
        {applicationsError && <p className="form-error" role="alert">Applications could not be loaded, so this task cannot be linked: {applicationsError}</p>}
        {error && <p className="form-error" role="alert">{error}</p>}
      </section>
      <div className="form-actions">
        {onCancel && <button className="button button-outline" disabled={busy} onClick={onCancel} type="button">Cancel</button>}
        <button className="button" disabled={busy} type="submit">{busy ? "Saving…" : submitLabel}</button>
      </div>
    </form>
  );
}
