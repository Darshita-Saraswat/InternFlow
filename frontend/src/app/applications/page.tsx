"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { ApplicationWorkspace } from "@/components/application-workspace";
import { ApiError } from "@/lib/auth-api";
import { getApplications } from "@/lib/applications-api";
import type { Application, ApplicationStatus } from "@/lib/applications-api";

const statuses: Array<ApplicationStatus | ""> = [
  "", "SAVED", "PREPARING", "APPLIED", "ONLINE_ASSESSMENT", "INTERVIEW", "OFFER", "REJECTED", "WITHDRAWN",
];

function labelFor(value: string) {
  return value ? value.replaceAll("_", " ").replace(/\b\w/g, (letter) => letter.toUpperCase()) : "All statuses";
}

function dateLabel(value: string | null) {
  if (!value) return "No deadline";
  return new Intl.DateTimeFormat(undefined, { dateStyle: "medium" }).format(new Date(`${value}T00:00:00`));
}

export default function ApplicationsPage() {
  const [items, setItems] = useState<Application[]>([]);
  const [queryInput, setQueryInput] = useState("");
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState<ApplicationStatus | "">("");
  const [page, setPage] = useState(0);
  const [totalItems, setTotalItems] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [retryCount, setRetryCount] = useState(0);

  useEffect(() => {
    let current = true;
    getApplications({ query, status, page, size: 20 })
      .then((result) => {
        if (!current) return;
        setItems(result.items);
        setTotalItems(result.totalItems);
        setError("");
      })
      .catch((caught: unknown) => {
        if (!current || (caught instanceof ApiError && caught.status === 401)) return;
        setError(caught instanceof Error ? caught.message : "Applications could not be loaded.");
      })
      .finally(() => {
        if (current) setLoading(false);
      });
    return () => { current = false; };
  }, [query, status, page, retryCount]);

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
    setPage(0);
  }

  const lastPage = Math.max(0, Math.ceil(totalItems / 20) - 1);

  return (
    <ApplicationWorkspace
      actions={<Link className="button" href="/applications/new">Add application <span aria-hidden="true">+</span></Link>}
      description="Keep each opportunity, deadline, and next step in view."
      title="Applications"
    >
      <section aria-label="Application records" className="application-panel">
        <form className="application-filters" onSubmit={search}>
          <label className="field search-field">
            <span>Search applications</span>
            <input onChange={(event) => setQueryInput(event.target.value)} placeholder="Company or role" value={queryInput} />
          </label>
          <label className="field">
            <span>Filter by status</span>
            <select onChange={(event) => { setLoading(true); setPage(0); setStatus(event.target.value as ApplicationStatus | ""); }} value={status}>
              {statuses.map((value) => <option key={value || "all"} value={value}>{labelFor(value)}</option>)}
            </select>
          </label>
          <button className="button" type="submit">Search</button>
          {(query || status) && <button className="button button-outline" onClick={clearFilters} type="button">Clear filters</button>}
        </form>

        {loading ? (
          <div className="application-state" aria-live="polite"><span className="spinner workspace-spinner" aria-hidden="true" /> Loading your applications…</div>
        ) : error ? (
          <div className="application-state" role="alert"><h2>Couldn’t load applications</h2><p>{error}</p><button className="button button-outline" onClick={() => { setLoading(true); setRetryCount((count) => count + 1); }} type="button">Try again</button></div>
        ) : items.length === 0 ? (
          <div className="application-state">
            <span aria-hidden="true" className="empty-mark">↗</span>
            <h2>{query || status ? "No applications match these filters." : "Your tracker is ready."}</h2>
            <p>{query || status ? "Try another search or clear the filters." : "Add your first opportunity to keep its dates, status, and next action together."}</p>
            {query || status
              ? <button className="button button-outline" onClick={clearFilters} type="button">Clear filters</button>
              : <Link className="button" href="/applications/new">Add your first application</Link>}
          </div>
        ) : (
          <>
            <ul className="application-list">
              {items.map((application) => (
                <li key={application.id}>
                  <Link className="application-card" href={`/applications/${application.id}`}>
                    <span className="application-card-main">
                      <span className="application-company">{application.company}</span>
                      <span className="application-role">{application.roleTitle}</span>
                      <span className="application-meta">
                        {labelFor(application.roleType)}
                        {application.location ? ` · ${application.location}` : ""}
                        {application.workMode ? ` · ${labelFor(application.workMode)}` : ""}
                      </span>
                    </span>
                    <span className={`status-pill status-${application.status.toLowerCase()}`}>{labelFor(application.status)}</span>
                    <span className="application-date"><span>Deadline</span>{dateLabel(application.deadline)}</span>
                    <span aria-hidden="true" className="application-arrow">→</span>
                  </Link>
                </li>
              ))}
            </ul>
            <div className="pagination-row">
              <span>{totalItems} {totalItems === 1 ? "application" : "applications"}</span>
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
