import { apiRequest } from "@/lib/auth-api";

export type OpportunityRoleType = "internship" | "graduate_role" | "analyst_role" | "early_career_other";
export type OpportunityWorkMode = "in_person" | "hybrid" | "remote";

export interface Opportunity {
  id: string;
  company: string;
  roleTitle: string;
  roleType: OpportunityRoleType;
  url: string | null;
  source: string | null;
  location: string | null;
  workMode: OpportunityWorkMode | null;
  description: string | null;
  deadline: string | null;
  eligibleGraduationFrom: number | null;
  eligibleGraduationTo: number | null;
  requiredSkills: string[];
  compensationMin: number | null;
  compensationMax: number | null;
  compensationCurrency: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface OpportunityPage {
  items: Opportunity[];
  page: number;
  size: number;
  totalItems: number;
}

export type FitMatchStatus = "MATCH" | "PARTIAL_MATCH" | "NO_MATCH" | "UNKNOWN";
export type GraduationEligibility = "ELIGIBLE" | "INELIGIBLE" | "UNKNOWN";

export interface FitMatchResult {
  status: FitMatchStatus;
  explanation: string;
}

export interface FitAssessment {
  opportunityId: string;
  skillsStatus: FitMatchStatus;
  skillsMatched: string[];
  requiredSkillsNotListed: string[];
  totalRequiredSkills: number | null;
  skillsMatchCount: number | null;
  skillsMatchPercentage: number | null;
  skillsExplanation: string;
  graduationEligibility: GraduationEligibility;
  graduationExplanation: string;
  roleAlignment: FitMatchResult;
  locationPreferenceMatch: FitMatchResult;
  workModePreferenceMatch: FitMatchResult;
  explanations: string[];
  suggestedNextSteps: string[];
}

async function readJson<T>(response: Response): Promise<T> {
  return (await response.json()) as T;
}

export async function getOpportunities(filters: {
  query?: string;
  roleType?: OpportunityRoleType | "";
  location?: string;
  workMode?: OpportunityWorkMode | "";
  page?: number;
  size?: number;
} = {}): Promise<OpportunityPage> {
  const params = new URLSearchParams();
  if (filters.query?.trim()) params.set("q", filters.query.trim());
  if (filters.roleType) params.set("roleType", filters.roleType);
  if (filters.location?.trim()) params.set("location", filters.location.trim());
  if (filters.workMode) params.set("workMode", filters.workMode);
  if (filters.page !== undefined) params.set("page", String(filters.page));
  if (filters.size !== undefined) params.set("size", String(filters.size));
  const suffix = params.size ? `?${params.toString()}` : "";
  return readJson<OpportunityPage>(await apiRequest(`opportunities${suffix}`));
}

export async function getOpportunity(id: string): Promise<Opportunity> {
  return readJson<Opportunity>(await apiRequest(`opportunities/${encodeURIComponent(id)}`));
}

export async function assessOpportunityFit(id: string): Promise<FitAssessment> {
  return readJson<FitAssessment>(
    await apiRequest(`opportunities/${encodeURIComponent(id)}/fit-assessment`),
  );
}
