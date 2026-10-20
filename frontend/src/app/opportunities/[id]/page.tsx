"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { ApplicationWorkspace } from "@/components/application-workspace";
import { ApiError } from "@/lib/auth-api";
import { assessOpportunityFit, getOpportunity } from "@/lib/opportunities-api";
import type { FitAssessment, Opportunity } from "@/lib/opportunities-api";

function labelFor(value: string) {
  return value.replaceAll("_", " ").toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function compensationLabel(opportunity: Opportunity) {
  if (opportunity.compensationMin == null && opportunity.compensationMax == null) return "Not specified";
  return `${opportunity.compensationMin ?? "—"} – ${opportunity.compensationMax ?? "—"} ${opportunity.compensationCurrency ?? ""}`.trim();
}

export default function OpportunityDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const [id, setId] = useState("");
  const [opportunity, setOpportunity] = useState<Opportunity | null>(null);
  const [assessment, setAssessment] = useState<FitAssessment | null>(null);
  const [assessing, setAssessing] = useState(false);
  const [assessmentError, setAssessmentError] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [retryCount, setRetryCount] = useState(0);

  useEffect(() => {
    void params.then(({ id: opportunityId }) => setId(opportunityId));
  }, [params]);

  useEffect(() => {
    if (!id) return;
    let current = true;
    getOpportunity(id)
      .then((loaded) => {
        if (!current) return;
        setOpportunity(loaded);
        setError("");
      })
      .catch((caught: unknown) => {
        if (!current || (caught instanceof ApiError && caught.status === 401)) return;
        setError(caught instanceof Error ? caught.message : "This catalogue record could not be loaded.");
      })
      .finally(() => { if (current) setLoading(false); });
    return () => { current = false; };
  }, [id, retryCount]);

  async function assessFit() {
    if (!opportunity) return;
    setAssessing(true);
    setAssessmentError("");
    setAssessment(null);
    try {
      setAssessment(await assessOpportunityFit(opportunity.id));
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 401) return;
      setAssessmentError(caught instanceof Error ? caught.message : "Fit assessment could not be completed.");
    } finally {
      setAssessing(false);
    }
  }

  return (
    <ApplicationWorkspace
      activeSection="opportunities"
      description={opportunity?.company ?? "Review an opportunity catalogue record."}
      title={opportunity?.roleTitle ?? "Opportunity details"}
    >
      <section aria-label="Opportunity details" className="application-panel">
        <div className="panel-intro"><Link className="text-link" href="/opportunities">← Back to catalogue</Link></div>
        {loading ? (
          <div aria-live="polite" className="application-state"><span aria-hidden="true" className="spinner workspace-spinner" /> Loading opportunity…</div>
        ) : error ? (
          <div className="application-state" role="alert">
            <h2>Couldn’t open this opportunity</h2>
            <p>{error}</p>
            <button className="button button-outline" onClick={() => { setLoading(true); setRetryCount((count) => count + 1); }} type="button">Try again</button>
          </div>
        ) : opportunity ? (
          <>
            <div className="detail-summary">
              <div>
                <span className="status-pill">{labelFor(opportunity.roleType)}</span>
                <p>{opportunity.location ?? "Location not specified"}{opportunity.workMode ? ` · ${labelFor(opportunity.workMode)}` : ""}</p>
              </div>
              <aside><span>Application deadline</span><strong>{opportunity.deadline ?? "Not specified"}</strong></aside>
            </div>
            <p className="catalogue-disclaimer">Catalogue record only. Availability and listing details have not been independently verified.</p>
            <section aria-labelledby="fit-assessment-heading" className="fit-assessment-section">
              <div className="fit-assessment-action">
                <div>
                  <h2 id="fit-assessment-heading">Internship fit assessment</h2>
                  <p>Compare this catalogue record with the profile associated with your signed-in account.</p>
                </div>
                <button className="button" disabled={assessing} onClick={() => void assessFit()} type="button">
                  {assessing ? "Assessing…" : "Assess My Fit"}
                </button>
              </div>
              {assessing && (
                <div aria-live="polite" className="fit-assessment-state">
                  <span aria-hidden="true" className="spinner workspace-spinner" /> Comparing your profile with this opportunity…
                </div>
              )}
              {assessmentError && <p className="profile-feedback profile-feedback-error" role="alert">{assessmentError}</p>}
              {assessment && (
                <div aria-live="polite" className="fit-assessment-results">
                  <p className="fit-assessment-note">
                    This is a rule-based comparison, not a hiring prediction. Skills use trimmed, case-insensitive exact text matching; semantic equivalence is not inferred. Unknown means information was unavailable.
                  </p>
                  <dl className="detail-grid">
                    <div>
                      <dt>Skills alignment</dt>
                      <dd>{assessment.skillsStatus.replaceAll("_", " ")}{assessment.skillsMatchPercentage == null
                        ? " · percentage unavailable"
                        : ` · ${assessment.skillsMatchPercentage}% (${assessment.skillsMatchCount} of ${assessment.totalRequiredSkills})`}</dd>
                      <p>{assessment.skillsExplanation}</p>
                    </div>
                    <div>
                      <dt>Skills listed in your profile</dt>
                      <dd>{assessment.skillsMatched.length ? assessment.skillsMatched.join(", ") : "No listed matches"}</dd>
                    </div>
                    <div>
                      <dt>Required skills not listed</dt>
                      <dd>{assessment.requiredSkillsNotListed.length
                        ? assessment.requiredSkillsNotListed.join(", ")
                        : assessment.skillsStatus === "UNKNOWN" ? "Unknown / not assessed" : "None"}</dd>
                    </div>
                    <div>
                      <dt>Graduation eligibility</dt>
                      <dd>{labelFor(assessment.graduationEligibility)}</dd>
                      <p>{assessment.graduationExplanation}</p>
                    </div>
                    <div>
                      <dt>Role alignment</dt>
                      <dd>{labelFor(assessment.roleAlignment.status)}</dd>
                      <p>{assessment.roleAlignment.explanation}</p>
                    </div>
                    <div>
                      <dt>Location preference</dt>
                      <dd>{labelFor(assessment.locationPreferenceMatch.status)}</dd>
                      <p>{assessment.locationPreferenceMatch.explanation}</p>
                    </div>
                    <div>
                      <dt>Work-mode preference</dt>
                      <dd>{labelFor(assessment.workModePreferenceMatch.status)}</dd>
                      <p>{assessment.workModePreferenceMatch.explanation}</p>
                    </div>
                  </dl>
                  <div className="fit-assessment-next-steps">
                    <h3>Suggested next steps</h3>
                    <ul>{assessment.suggestedNextSteps.map((step) => <li key={step}>{step}</li>)}</ul>
                  </div>
                </div>
              )}
            </section>
            <dl className="detail-grid">
              <div><dt>Role type</dt><dd>{labelFor(opportunity.roleType)}</dd></div>
              <div><dt>Source</dt><dd>{opportunity.source ? labelFor(opportunity.source) : "Not specified"}</dd></div>
              <div><dt>Work mode</dt><dd>{opportunity.workMode ? labelFor(opportunity.workMode) : "Not specified"}</dd></div>
              <div><dt>Eligible graduation years</dt><dd>{opportunity.eligibleGraduationFrom ?? "—"} – {opportunity.eligibleGraduationTo ?? "—"}</dd></div>
              <div><dt>Compensation</dt><dd>{compensationLabel(opportunity)}</dd></div>
              <div><dt>Required skills</dt><dd>{opportunity.requiredSkills.length ? opportunity.requiredSkills.join(", ") : "Not specified"}</dd></div>
              <div className="detail-wide"><dt>Description</dt><dd className="detail-notes">{opportunity.description || "No description provided."}</dd></div>
              <div className="detail-wide"><dt>Opportunity link</dt><dd>{opportunity.url
                ? <a href={opportunity.url} rel="noreferrer" target="_blank">Visit the listed website</a>
                : "Not provided"}</dd></div>
            </dl>
          </>
        ) : (
          <div className="application-state"><h2>Opportunity unavailable</h2></div>
        )}
      </section>
    </ApplicationWorkspace>
  );
}
