"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { ApplicationForm } from "@/components/application-form";
import { ApplicationWorkspace } from "@/components/application-workspace";
import { ApiError } from "@/lib/auth-api";
import { deleteApplication, getApplication, updateApplication } from "@/lib/applications-api";
import type { ApplicationDetail } from "@/lib/applications-api";

function labelFor(value: string) {
  return value.replaceAll("_", " ").replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function dateTimeLabel(value: string) {
  return new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}

export default function ApplicationDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const router = useRouter();
  const [id, setId] = useState("");
  const [detail, setDetail] = useState<ApplicationDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [editing, setEditing] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [retryCount, setRetryCount] = useState(0);

  useEffect(() => {
    void params.then(({ id: applicationId }) => setId(applicationId));
  }, [params]);

  useEffect(() => {
    if (!id) return;
    let current = true;
    getApplication(id)
      .then((applicationDetail) => {
        if (!current) return;
        setDetail(applicationDetail);
        setError("");
      })
      .catch((caught: unknown) => {
        if (!current || (caught instanceof ApiError && caught.status === 401)) return;
        setError(caught instanceof Error ? caught.message : "This application could not be loaded.");
      })
      .finally(() => {
        if (current) setLoading(false);
      });
    return () => { current = false; };
  }, [id, retryCount]);

  async function remove() {
    if (!detail || !window.confirm(`Delete the application with ${detail.application.company}? This cannot be undone.`)) return;
    setDeleting(true);
    setError("");
    try {
      await deleteApplication(detail.application.id);
      router.push("/applications");
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "The application could not be deleted.");
      setDeleting(false);
    }
  }

  const title = detail?.application.roleTitle ?? "Application details";

  return (
    <ApplicationWorkspace
      actions={detail && !editing ? (
        <div className="workspace-actions-inline">
          <button className="button button-outline" onClick={() => setEditing(true)} type="button">Edit details</button>
          <button className="button button-danger" disabled={deleting} onClick={() => void remove()} type="button">{deleting ? "Deleting…" : "Delete"}</button>
        </div>
      ) : undefined}
      description={detail?.application.company ?? "Review the record and its activity."}
      title={title}
    >
      <section className="application-panel">
        <div className="panel-intro">
          <Link className="text-link" href="/applications">← Back to applications</Link>
        </div>
        {loading ? (
          <div className="application-state" aria-live="polite"><span className="spinner workspace-spinner" aria-hidden="true" /> Loading application…</div>
        ) : error ? (
          <div className="application-state" role="alert"><h2>Couldn’t open this application</h2><p>{error}</p><button className="button button-outline" onClick={() => { setLoading(true); setRetryCount((count) => count + 1); }} type="button">Try again</button></div>
        ) : detail && editing ? (
          <ApplicationForm
            initial={detail.application}
            onCancel={() => setEditing(false)}
            onSubmit={async (input) => {
              await updateApplication(detail.application.id, input);
              setDetail(await getApplication(detail.application.id));
              setEditing(false);
            }}
            submitLabel="Save changes"
          />
        ) : detail ? (
          <>
            <div className="detail-summary">
              <div>
                <span className={`status-pill status-${detail.application.status.toLowerCase()}`}>{labelFor(detail.application.status)}</span>
                <span className={`priority-pill priority-${detail.application.priority.toLowerCase()}`}>{labelFor(detail.application.priority)} priority</span>
                <p>{labelFor(detail.application.roleType)}{detail.application.location ? ` · ${detail.application.location}` : ""}{detail.application.workMode ? ` · ${labelFor(detail.application.workMode)}` : ""}</p>
              </div>
              {detail.application.nextAction && <aside><span>Next action</span><strong>{detail.application.nextAction}</strong></aside>}
            </div>
            {error && <p className="form-error" role="alert">{error}</p>}
            <dl className="detail-grid">
              <div><dt>Saved on</dt><dd>{detail.application.savedOn}</dd></div>
              <div><dt>Deadline</dt><dd>{detail.application.deadline ?? "Not set"}</dd></div>
              <div><dt>Applied on</dt><dd>{detail.application.appliedOn ?? "Not set"}</dd></div>
              <div><dt>Follow-up</dt><dd>{detail.application.followUpOn ?? "Not set"}</dd></div>
              <div><dt>Assessment</dt><dd>{detail.application.assessmentAt ? dateTimeLabel(detail.application.assessmentAt) : "Not set"}</dd></div>
              <div><dt>Interview</dt><dd>{detail.application.interviewAt ? dateTimeLabel(detail.application.interviewAt) : "Not set"}</dd></div>
              <div><dt>Source</dt><dd>{detail.application.source ? labelFor(detail.application.source) : "Not set"}</dd></div>
              <div><dt>Job link</dt><dd>{detail.application.url ? <a href={detail.application.url} rel="noreferrer" target="_blank">{detail.application.url}</a> : "Not set"}</dd></div>
              <div><dt>Compensation</dt><dd>{detail.application.compensationMin != null || detail.application.compensationMax != null
                ? `${detail.application.compensationMin ?? "—"} – ${detail.application.compensationMax ?? "—"} ${detail.application.compensationCurrency ?? ""}`
                : "Not set"}</dd></div>
              <div><dt>Skills</dt><dd>{detail.application.requiredSkills.length ? detail.application.requiredSkills.join(", ") : "Not set"}</dd></div>
              <div className="detail-wide"><dt>Document reference</dt><dd>{detail.application.documentReference ?? "Not set"}</dd></div>
              <div className="detail-wide"><dt>Notes</dt><dd className="detail-notes">{detail.application.notes || "No notes yet."}</dd></div>
            </dl>
            <section className="activity-section">
              <h2>Activity</h2>
              {detail.events.length ? (
                <ol className="activity-list">
                  {detail.events.map((event) => (
                    <li key={event.id}>
                      <span className="activity-dot" aria-hidden="true" />
                      <div><strong>{event.description ?? labelFor(event.eventType)}</strong><span>{dateTimeLabel(event.occurredAt)}</span></div>
                    </li>
                  ))}
                </ol>
              ) : <p>No activity has been recorded yet.</p>}
            </section>
          </>
        ) : (
          <div className="application-state"><h2>Application unavailable</h2><Link className="text-link" href="/applications">Back to applications</Link></div>
        )}
      </section>
    </ApplicationWorkspace>
  );
}
