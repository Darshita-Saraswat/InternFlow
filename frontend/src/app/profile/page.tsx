"use client";

import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { ApplicationWorkspace } from "@/components/application-workspace";
import { ApiError } from "@/lib/auth-api";
import { getStudentProfile, updateStudentProfile } from "@/lib/profile-api";
import type { StudentProfile, WorkMode } from "@/lib/profile-api";

const workModeOptions: Array<{ value: WorkMode; label: string }> = [
  { value: "in_person", label: "In person" },
  { value: "hybrid", label: "Hybrid" },
  { value: "remote", label: "Remote" },
];

interface ProfileForm {
  university: string;
  degree: string;
  graduationDate: string;
  studyYear: string;
  targetRoleCategories: string;
  skills: string;
  preferredLocations: string;
  workModes: WorkMode[];
}

const emptyForm: ProfileForm = {
  university: "",
  degree: "",
  graduationDate: "",
  studyYear: "",
  targetRoleCategories: "",
  skills: "",
  preferredLocations: "",
  workModes: [],
};

function splitValues(value: string): string[] {
  return [...new Set(value.split(",").map((item) => item.trim()).filter(Boolean))];
}

function toForm(profile: StudentProfile): ProfileForm {
  return {
    university: profile.university ?? "",
    degree: profile.degree ?? "",
    graduationDate: profile.graduationDate ?? "",
    studyYear: profile.studyYear?.toString() ?? "",
    targetRoleCategories: profile.targetRoleCategories.join(", "),
    skills: profile.skills.join(", "),
    preferredLocations: profile.preferredLocations.join(", "),
    workModes: profile.workModes,
  };
}

function validateForm(form: ProfileForm): string {
  if (form.university.trim().length > 200) return "University must be 200 characters or fewer.";
  if (form.degree.trim().length > 200) return "Degree must be 200 characters or fewer.";
  if (form.studyYear) {
    const year = Number(form.studyYear);
    if (!Number.isInteger(year) || year < 1 || year > 10) return "Study year must be a whole number from 1 to 10.";
  }

  const fields = [
    { label: "Target roles", value: form.targetRoleCategories, maxCount: 20, maxLength: 80 },
    { label: "Skills", value: form.skills, maxCount: 50, maxLength: 80 },
    { label: "Preferred locations", value: form.preferredLocations, maxCount: 20, maxLength: 100 },
  ];
  for (const field of fields) {
    const values = splitValues(field.value);
    if (values.length > field.maxCount) return `${field.label} can contain at most ${field.maxCount} items.`;
    if (values.some((value) => value.length > field.maxLength)) {
      return `Each ${field.label.toLowerCase()} item must be ${field.maxLength} characters or fewer.`;
    }
  }
  return "";
}

export default function ProfilePage() {
  const [form, setForm] = useState<ProfileForm>(emptyForm);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [loadError, setLoadError] = useState("");
  const [saveError, setSaveError] = useState("");
  const [success, setSuccess] = useState("");
  const [retryCount, setRetryCount] = useState(0);

  useEffect(() => {
    let current = true;
    getStudentProfile()
      .then((profile) => {
        if (!current) return;
        setForm(toForm(profile));
        setLoadError("");
      })
      .catch((caught: unknown) => {
        if (!current || (caught instanceof ApiError && caught.status === 401)) return;
        setLoadError(caught instanceof Error ? caught.message : "Your profile could not be loaded.");
      })
      .finally(() => {
        if (current) setLoading(false);
      });
    return () => { current = false; };
  }, [retryCount]);

  function change<K extends keyof ProfileForm>(key: K, value: ProfileForm[K]) {
    setForm((current) => ({ ...current, [key]: value }));
    setSaveError("");
    setSuccess("");
  }

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaveError("");
    setSuccess("");
    const validationError = validateForm(form);
    if (validationError) {
      setSaveError(validationError);
      return;
    }

    setSaving(true);
    try {
      const profile = await updateStudentProfile({
        university: form.university.trim() || null,
        degree: form.degree.trim() || null,
        graduationDate: form.graduationDate || null,
        studyYear: form.studyYear ? Number(form.studyYear) : null,
        targetRoleCategories: splitValues(form.targetRoleCategories),
        skills: splitValues(form.skills),
        preferredLocations: splitValues(form.preferredLocations),
        workModes: form.workModes,
      });
      setForm(toForm(profile));
      setSuccess("Your profile has been saved.");
    } catch (caught) {
      setSaveError(caught instanceof Error ? caught.message : "Your profile could not be saved.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <ApplicationWorkspace activeSection="profile" description="Keep your academic and career preferences up to date." title="Your profile">
      {loading ? (
        <section aria-live="polite" className="application-state">
          <span aria-hidden="true" className="spinner workspace-spinner" />
          Loading your profile…
        </section>
      ) : loadError ? (
        <section className="application-state" role="alert">
          <h2>We couldn’t load your profile.</h2>
          <p>{loadError}</p>
          <button className="button button-outline" onClick={() => { setLoading(true); setRetryCount((count) => count + 1); }} type="button">
            Try again
          </button>
        </section>
      ) : (
        <section className="application-panel profile-panel">
          <div className="profile-intro">
            <p>These details belong to your signed-in account. Email and display name are managed with your account credentials.</p>
          </div>
          <form className="application-form profile-form" onSubmit={(event) => void save(event)}>
            <section className="form-section" aria-labelledby="profile-study-heading">
              <h2 id="profile-study-heading">Education</h2>
              <div className="application-fields">
                <label className="field">
                  <span>University</span>
                  <input autoComplete="organization" maxLength={200} onChange={(event) => change("university", event.target.value)} value={form.university} />
                </label>
                <label className="field">
                  <span>Degree or program</span>
                  <input maxLength={200} onChange={(event) => change("degree", event.target.value)} value={form.degree} />
                </label>
                <label className="field">
                  <span>Graduation date</span>
                  <input onChange={(event) => change("graduationDate", event.target.value)} type="date" value={form.graduationDate} />
                </label>
                <label className="field">
                  <span>Current study year</span>
                  <input inputMode="numeric" max={10} min={1} onChange={(event) => change("studyYear", event.target.value)} placeholder="e.g. 3" type="number" value={form.studyYear} />
                </label>
              </div>
            </section>

            <section className="form-section" aria-labelledby="profile-preferences-heading">
              <h2 id="profile-preferences-heading">Career preferences</h2>
              <div className="application-fields">
                <label className="field">
                  <span>Target role categories</span>
                  <textarea maxLength={1700} onChange={(event) => change("targetRoleCategories", event.target.value)} placeholder="Software engineering, Data analysis" rows={3} value={form.targetRoleCategories} />
                  <small>Separate items with commas.</small>
                </label>
                <label className="field">
                  <span>Skills</span>
                  <textarea maxLength={4100} onChange={(event) => change("skills", event.target.value)} placeholder="Java, SQL, Research" rows={3} value={form.skills} />
                  <small>Separate items with commas.</small>
                </label>
                <label className="field">
                  <span>Preferred locations</span>
                  <textarea maxLength={2030} onChange={(event) => change("preferredLocations", event.target.value)} placeholder="Boston, Remote" rows={3} value={form.preferredLocations} />
                  <small>Separate items with commas.</small>
                </label>
                <fieldset className="profile-work-modes">
                  <legend>Work modes</legend>
                  <div>
                    {workModeOptions.map((option) => (
                      <label className="profile-checkbox" key={option.value}>
                        <input
                          checked={form.workModes.includes(option.value)}
                          onChange={(event) => change(
                            "workModes",
                            event.target.checked
                              ? [...form.workModes, option.value]
                              : form.workModes.filter((mode) => mode !== option.value),
                          )}
                          type="checkbox"
                        />
                        <span>{option.label}</span>
                      </label>
                    ))}
                  </div>
                </fieldset>
              </div>
            </section>
            {saveError && <p className="profile-feedback profile-feedback-error" role="alert">{saveError}</p>}
            {success && <p className="profile-feedback profile-feedback-success" role="status">{success}</p>}
            <div className="form-actions">
              <button className="button" disabled={saving} type="submit">
                {saving ? "Saving…" : "Save profile"}
              </button>
            </div>
          </form>
        </section>
      )}
    </ApplicationWorkspace>
  );
}
