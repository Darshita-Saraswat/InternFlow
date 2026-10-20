import { apiRequest } from "@/lib/auth-api";

export type WorkMode = "in_person" | "hybrid" | "remote";

export interface StudentProfile {
  university: string | null;
  degree: string | null;
  graduationDate: string | null;
  studyYear: number | null;
  targetRoleCategories: string[];
  skills: string[];
  preferredLocations: string[];
  workModes: WorkMode[];
}

export async function getStudentProfile(): Promise<StudentProfile> {
  const response = await apiRequest("profile");
  return (await response.json()) as StudentProfile;
}

export async function updateStudentProfile(input: StudentProfile): Promise<StudentProfile> {
  const response = await apiRequest("profile", { method: "PUT", body: input });
  return (await response.json()) as StudentProfile;
}
