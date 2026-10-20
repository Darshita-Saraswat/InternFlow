import { apiRequest } from "@/lib/auth-api";

export type ApplicationStatus =
  | "SAVED"
  | "PREPARING"
  | "APPLIED"
  | "ONLINE_ASSESSMENT"
  | "INTERVIEW"
  | "OFFER"
  | "REJECTED"
  | "WITHDRAWN";

export type ApplicationPriority = "LOW" | "NORMAL" | "HIGH";
export type RoleType = "internship" | "graduate_role" | "analyst_role" | "early_career_other";
export type WorkMode = "in_person" | "hybrid" | "remote";
export type ApplicationSource = "careers_page" | "campus_placement" | "referral" | "job_board" | "event" | "other";

export interface ApplicationInput {
  company: string;
  roleTitle: string;
  roleType: RoleType;
  url: string | null;
  source: ApplicationSource | null;
  location: string | null;
  workMode: WorkMode | null;
  savedOn: string;
  deadline: string | null;
  appliedOn: string | null;
  followUpOn: string | null;
  assessmentAt: string | null;
  interviewAt: string | null;
  status: ApplicationStatus;
  priority: ApplicationPriority;
  nextAction: string | null;
  compensationMin: number | null;
  compensationMax: number | null;
  compensationCurrency: string | null;
  requiredSkills: string[];
  notes: string | null;
  documentReference: string | null;
}

export interface Application extends ApplicationInput {
  id: string;
  createdAt: string;
  updatedAt: string;
}

export interface ApplicationEvent {
  id: string;
  eventType: string;
  statusBefore: ApplicationStatus | null;
  statusAfter: ApplicationStatus | null;
  description: string | null;
  occurredAt: string;
}

export interface ApplicationPage {
  items: Application[];
  page: number;
  size: number;
  totalItems: number;
}

export interface ApplicationDetail {
  application: Application;
  events: ApplicationEvent[];
}

async function readJson<T>(response: Response): Promise<T> {
  return (await response.json()) as T;
}

export async function getApplications(filters: {
  query?: string;
  status?: ApplicationStatus | "";
  page?: number;
  size?: number;
} = {}): Promise<ApplicationPage> {
  const params = new URLSearchParams();
  if (filters.query?.trim()) params.set("q", filters.query.trim());
  if (filters.status) params.set("status", filters.status);
  if (filters.page !== undefined) params.set("page", String(filters.page));
  if (filters.size !== undefined) params.set("size", String(filters.size));
  const suffix = params.size ? `?${params.toString()}` : "";
  return readJson<ApplicationPage>(await apiRequest(`applications${suffix}`));
}

export async function getApplication(id: string): Promise<ApplicationDetail> {
  return readJson<ApplicationDetail>(await apiRequest(`applications/${encodeURIComponent(id)}`));
}

export async function createApplication(input: ApplicationInput): Promise<Application> {
  return readJson<Application>(await apiRequest("applications", { method: "POST", body: input }));
}

export async function updateApplication(id: string, input: ApplicationInput): Promise<Application> {
  return readJson<Application>(
    await apiRequest(`applications/${encodeURIComponent(id)}`, { method: "PUT", body: input }),
  );
}

export async function deleteApplication(id: string): Promise<void> {
  await apiRequest(`applications/${encodeURIComponent(id)}`, { method: "DELETE" });
}
