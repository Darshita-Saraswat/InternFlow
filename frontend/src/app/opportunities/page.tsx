"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { ApplicationWorkspace } from "@/components/application-workspace";
import { ApiError } from "@/lib/auth-api";
import { getOpportunities } from "@/lib/opportunities-api";
import type {
  Opportunity,
  OpportunityPage,
  OpportunityRoleType,
  OpportunityWorkMode,
} from "@/lib/opportunities-api";

const roleTypes: Array<OpportunityRoleType | ""> = [
  "", "internship", "graduate_role", "analyst_role", "early_career_other",
];
const workModes: Array<OpportunityWorkMode | ""> = ["", "in_person", "hybrid", "remote"];

function labelFor(value: string) {
  return value
    ? value.replaceAll("_", " ").toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase())
    : "All";
}

function deadlineLabel(value: string | null) {
  if (!value) return "No deadline";
  return new Intl.DateTimeFormat(undefined, { dateStyle: "medium" }).format(new Date(`${value}T00:00:00`));
}

export default function OpportunitiesPage() {
  const [result, setResult] = useState<OpportunityPage>({ items: [], page: 0, size: 20, totalItems: 0 });
  const [queryInput, setQueryInput] = useState("");
  const [query, setQuery] = useState("");
  const [roleType, setRoleType] = useState<OpportunityRoleType | "">("");
  const [locationInput, setLocationInput] = useState("");
  const [location, setLocation] = useState("");
  const [workMode, setWorkMode] = useState<OpportunityWorkMode | "">("");
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [retryCount, setRetryCount] = useState(0);

  useEffect(() => {
    let current = true;
    getOpportunities({ query, roleType, location, workMode, page, size: 20 })
      .then((loaded) => {
        if (!current) return;
        setResult(loaded);
        setError("");
      })
      .catch((caught: unknown) => {
        if (!current || (caught instanceof ApiError && caught.status === 401)) return;
        setError(caught instanceof Error ? caught.message : "The opportunity catalogue could not be loaded.");
      })
      .finally(() => { if (current) setLoading(false); });
    return () => { current = false; };
  }, [query, roleType, location, workMode, page, retryCount]);

  function search(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setPage(0);
    setQuery(queryInput);
    setLocation(locationInput);
  }

  function clearFilters() {
    setLoading(true);
    setQuery("");
    setQueryInput("");
    setRoleType("");
    setLocation("");
    setLocationInput("");
    setWorkMode("");
    setPage(0);
  }

  const lastPage = Math.max(0, Math.ceil(result.totalItems / 20) - 1);
  const hasFilters = Boolean(query || roleType || location || workMode);

  return (
    <ApplicationWorkspace
      activeSection="opportunities"
      description="Browse records in InternFlow’s shared opportunity catalogue. Listings are not verified for current availability."
      title="Opportunity catalogue"
    >
      <section aria-label="Opportunity catalogue" className="application-panel">
        <form className="application-filters task-filters opportunity-filters" onSubmit={search}>
          <label className="field search-field">
            <span>Search company or role</span>
            <input maxLength={200} onChange={(event) => setQueryInput(event.target.value)} placeholder="Company or role title" value={queryInput} />
          </label>
          <label className="field">
            <span>Role type</span>
            <select onChange={(event) => { setLoading(true); setPage(0); setRoleType(event.target.value as OpportunityRoleType | ""); }} value={roleType}>
              {roleTypes.map((value) => <option key={value || "all"} value={value}>{labelFor(value)}{value ? "" : " role types"}</option>)}
            </select>
          </label>
          <label className="field">
            <span>Location</span>
            <input maxLength={200} onChange={(event) => setLocationInput(event.target.value)} placeholder="Any location" value={locationInput} />
          </label>
          <label className="field">
            <span>Work mode</span>
            <select onChange={(event) => { setLoading(true); setPage(0); setWorkMode(event.target.value as OpportunityWorkMode | ""); }} value={workMode}>
              {workModes.map((value) => <option key={value || "all"} value={value}>{labelFor(value)}{value ? "" : " work modes"}</option>)}
            </select>
          </label>
          <button className="button" type="submit">Search</button>
          {hasFilters && <button className="button button-outline" onClick={clearFilters} type="button">Clear filters</button>}
        </form>

        {loading ? (
          <div aria-live="polite" className="application-state"><span aria-hidden="true" className="spinner workspace-spinner" /> Loading catalogue opportunities…</div>
        ) : error ? (
          <div className="application-state" role="alert">
            <h2>Couldn’t load the catalogue</h2>
            <p>{error}</p>
            <button className="button button-outline" onClick={() => { setLoading(true); setRetryCount((count) => count + 1); }} type="button">Try again</button>
          </div>
        ) : result.items.length === 0 ? (
          <div className="application-state">
            <span aria-hidden="true" className="empty-mark">↗</span>
            <h2>{hasFilters ? "No catalogue records match these filters." : "The catalogue is empty."}</h2>
            <p>{hasFilters ? "Try a different search or clear the filters." : "No opportunity records have been added yet."}</p>
            {hasFilters && <button className="button button-outline" onClick={clearFilters} type="button">Clear filters</button>}
          </div>
        ) : (
          <>
            <ul className="application-list">
              {result.items.map((opportunity: Opportunity) => (
                <li key={opportunity.id}>
                  <Link className="application-card opportunity-card" href={`/opportunities/${opportunity.id}`}>
                    <span className="application-card-main">
                      <span className="application-company">{opportunity.company}</span>
                      <span className="application-role">{opportunity.roleTitle}</span>
                      <span className="application-meta">
                        {labelFor(opportunity.roleType)}
                        {opportunity.location ? ` · ${opportunity.location}` : ""}
                        {opportunity.workMode ? ` · ${labelFor(opportunity.workMode)}` : ""}
                      </span>
                    </span>
                    <span className="status-pill">{labelFor(opportunity.roleType)}</span>
                    <span className="application-date"><span>Deadline</span>{deadlineLabel(opportunity.deadline)}</span>
                    <span aria-hidden="true" className="application-arrow">→</span>
                  </Link>
                </li>
              ))}
            </ul>
            <div className="pagination-row">
              <span>{result.totalItems} catalogue {result.totalItems === 1 ? "record" : "records"}</span>
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
