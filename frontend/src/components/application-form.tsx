"use client";

import { useState } from "react";
import type { FormEvent } from "react";
import type {
  Application,
  ApplicationInput,
  ApplicationPriority,
  ApplicationSource,
  ApplicationStatus,
  RoleType,
  WorkMode,
} from "@/lib/applications-api";

type Draft = Record<keyof ApplicationInput, string>;

const statuses: ApplicationStatus[] = [
  "SAVED", "PREPARING", "APPLIED", "ONLINE_ASSESSMENT", "INTERVIEW", "OFFER", "REJECTED", "WITHDRAWN",
];
const roleTypes: RoleType[] = ["internship", "graduate_role", "analyst_role", "early_career_other"];
const sources: ApplicationSource[] = ["careers_page", "campus_placement", "referral", "job_board", "event", "other"];
const workModes: WorkMode[] = ["in_person", "hybrid", "remote"];
const priorities: ApplicationPriority[] = ["LOW", "NORMAL", "HIGH"];

function todayLocal() {
  const today = new Date();
  return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`;
}

function localDateTime(value: string | null) {
  if (!value) return "";
  const date = new Date(value);
  const part = (number: number) => String(number).padStart(2, "0");
  return `${date.getFullYear()}-${part(date.getMonth() + 1)}-${part(date.getDate())}T${part(date.getHours())}:${part(date.getMinutes())}`;
}

function toDraft(application?: Application): Draft {
  return {
    company: application?.company ?? "",
    roleTitle: application?.roleTitle ?? "",
    roleType: application?.roleType ?? "internship",
    url: application?.url ?? "",
    source: application?.source ?? "",
    location: application?.location ?? "",
    workMode: application?.workMode ?? "",
    savedOn: application?.savedOn ?? todayLocal(),
    deadline: application?.deadline ?? "",
    appliedOn: application?.appliedOn ?? "",
    followUpOn: application?.followUpOn ?? "",
    assessmentAt: localDateTime(application?.assessmentAt ?? null),
    interviewAt: localDateTime(application?.interviewAt ?? null),
    status: application?.status ?? "SAVED",
    priority: application?.priority ?? "NORMAL",
    nextAction: application?.nextAction ?? "",
    compensationMin: application?.compensationMin?.toString() ?? "",
    compensationMax: application?.compensationMax?.toString() ?? "",
    compensationCurrency: application?.compensationCurrency ?? "",
    requiredSkills: application?.requiredSkills.join(", ") ?? "",
    notes: application?.notes ?? "",
    documentReference: application?.documentReference ?? "",
  };
}

function optional(value: string) {
  return value.trim() || null;
}

function toInput(draft: Draft): ApplicationInput {
  return {
    company: draft.company.trim(),
    roleTitle: draft.roleTitle.trim(),
    roleType: draft.roleType as RoleType,
    url: optional(draft.url),
    source: optional(draft.source) as ApplicationSource | null,
    location: optional(draft.location),
    workMode: optional(draft.workMode) as WorkMode | null,
    savedOn: draft.savedOn,
    deadline: optional(draft.deadline),
    appliedOn: optional(draft.appliedOn),
    followUpOn: optional(draft.followUpOn),
    assessmentAt: draft.assessmentAt ? new Date(draft.assessmentAt).toISOString() : null,
    interviewAt: draft.interviewAt ? new Date(draft.interviewAt).toISOString() : null,
    status: draft.status as ApplicationStatus,
    priority: draft.priority as ApplicationPriority,
    nextAction: optional(draft.nextAction),
    compensationMin: draft.compensationMin ? Number(draft.compensationMin) : null,
    compensationMax: draft.compensationMax ? Number(draft.compensationMax) : null,
    compensationCurrency: optional(draft.compensationCurrency)?.toUpperCase() ?? null,
    requiredSkills: draft.requiredSkills.split(",").map((skill) => skill.trim()).filter(Boolean),
    notes: optional(draft.notes),
    documentReference: optional(draft.documentReference),
  };
}

function labelFor(value: string) {
  return value.replaceAll("_", " ").replace(/\b\w/g, (letter) => letter.toUpperCase());
}

export function ApplicationForm({
  initial,
  submitLabel,
  onSubmit,
  onCancel,
}: {
  initial?: Application;
  submitLabel: string;
  onSubmit: (input: ApplicationInput) => Promise<void>;
  onCancel?: () => void;
}) {
  const [draft, setDraft] = useState<Draft>(() => toDraft(initial));
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  function update<K extends keyof Draft>(key: K, value: Draft[K]) {
    setDraft((current) => ({ ...current, [key]: value }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      await onSubmit(toInput(draft));
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Your application could not be saved.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="application-form" onSubmit={handleSubmit}>
      <section className="form-section">
        <h2>Role details</h2>
        <div className="application-fields">
          <label className="field"><span>Company *</span><input maxLength={200} onChange={(event) => update("company", event.target.value)} required value={draft.company} /></label>
          <label className="field"><span>Role title *</span><input maxLength={200} onChange={(event) => update("roleTitle", event.target.value)} required value={draft.roleTitle} /></label>
          <label className="field"><span>Role type *</span><select onChange={(event) => update("roleType", event.target.value)} value={draft.roleType}>{roleTypes.map((value) => <option key={value} value={value}>{labelFor(value)}</option>)}</select></label>
          <label className="field"><span>Application status *</span><select onChange={(event) => update("status", event.target.value)} value={draft.status}>{statuses.map((value) => <option key={value} value={value}>{labelFor(value)}</option>)}</select></label>
          <label className="field"><span>Priority *</span><select onChange={(event) => update("priority", event.target.value)} value={draft.priority}>{priorities.map((value) => <option key={value} value={value}>{labelFor(value)}</option>)}</select></label>
          <label className="field"><span>Source</span><select onChange={(event) => update("source", event.target.value)} value={draft.source}><option value="">Not specified</option>{sources.map((value) => <option key={value} value={value}>{labelFor(value)}</option>)}</select></label>
          <label className="field"><span>Job URL</span><input maxLength={2048} onChange={(event) => update("url", event.target.value)} placeholder="https://" type="url" value={draft.url} /></label>
          <label className="field"><span>Location</span><input maxLength={200} onChange={(event) => update("location", event.target.value)} value={draft.location} /></label>
          <label className="field"><span>Work mode</span><select onChange={(event) => update("workMode", event.target.value)} value={draft.workMode}><option value="">Not specified</option>{workModes.map((value) => <option key={value} value={value}>{labelFor(value)}</option>)}</select></label>
        </div>
      </section>

      <section className="form-section">
        <h2>Dates and next step</h2>
        <div className="application-fields">
          <label className="field"><span>Saved on *</span><input onChange={(event) => update("savedOn", event.target.value)} required type="date" value={draft.savedOn} /></label>
          <label className="field"><span>Deadline</span><input onChange={(event) => update("deadline", event.target.value)} type="date" value={draft.deadline} /></label>
          <label className="field"><span>Applied on</span><input onChange={(event) => update("appliedOn", event.target.value)} type="date" value={draft.appliedOn} /></label>
          <label className="field"><span>Follow-up date</span><input onChange={(event) => update("followUpOn", event.target.value)} type="date" value={draft.followUpOn} /></label>
          <label className="field"><span>Assessment date and time</span><input onChange={(event) => update("assessmentAt", event.target.value)} type="datetime-local" value={draft.assessmentAt} /></label>
          <label className="field"><span>Interview date and time</span><input onChange={(event) => update("interviewAt", event.target.value)} type="datetime-local" value={draft.interviewAt} /></label>
          <label className="field field-wide"><span>Next action</span><input maxLength={500} onChange={(event) => update("nextAction", event.target.value)} placeholder="e.g. Tailor resume and submit" value={draft.nextAction} /></label>
        </div>
      </section>

      <section className="form-section">
        <h2>Additional details</h2>
        <div className="application-fields">
          <label className="field"><span>Minimum compensation</span><input min="0" onChange={(event) => update("compensationMin", event.target.value)} step="0.01" type="number" value={draft.compensationMin} /></label>
          <label className="field"><span>Maximum compensation</span><input min="0" onChange={(event) => update("compensationMax", event.target.value)} step="0.01" type="number" value={draft.compensationMax} /></label>
          <label className="field"><span>Currency (3 letters)</span><input maxLength={3} onChange={(event) => update("compensationCurrency", event.target.value)} placeholder="USD" value={draft.compensationCurrency} /></label>
          <label className="field"><span>Skills (comma-separated)</span><input onChange={(event) => update("requiredSkills", event.target.value)} placeholder="Java, SQL, communication" value={draft.requiredSkills} /></label>
          <label className="field field-wide"><span>Resume or document reference</span><input maxLength={2048} onChange={(event) => update("documentReference", event.target.value)} value={draft.documentReference} /></label>
          <label className="field field-wide"><span>Notes</span><textarea maxLength={10000} onChange={(event) => update("notes", event.target.value)} rows={4} value={draft.notes} /></label>
        </div>
      </section>

      {error && <p className="form-error" role="alert">{error}</p>}
      <div className="form-actions">
        {onCancel && <button className="button button-outline" disabled={busy} onClick={onCancel} type="button">Cancel</button>}
        <button className="button" disabled={busy} type="submit">
          {busy ? <><span aria-hidden="true" className="spinner" /> Saving…</> : <>{submitLabel} <span aria-hidden="true">→</span></>}
        </button>
      </div>
    </form>
  );
}
