import { apiRequest } from "@/lib/auth-api";

export type TaskStatus = "TODO" | "IN_PROGRESS" | "COMPLETED";
export type TaskPriority = "LOW" | "NORMAL" | "HIGH";
export type TaskCategory =
  | "RESUME"
  | "RESEARCH"
  | "ONLINE_ASSESSMENT"
  | "DSA_PRACTICE"
  | "INTERVIEW_PREPARATION"
  | "NETWORKING"
  | "FOLLOW_UP"
  | "DOCUMENTS"
  | "OTHER";
export type TaskSort = "DUE_AT" | "CREATED_AT" | "UPDATED_AT";

export interface TaskInput {
  title: string;
  description: string | null;
  priority: TaskPriority;
  category: TaskCategory;
  status: TaskStatus;
  dueAt: string | null;
  applicationId: string | null;
}

export interface Task extends TaskInput {
  id: string;
  applicationCompany: string | null;
  applicationRoleTitle: string | null;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface TaskPage {
  items: Task[];
  page: number;
  size: number;
  totalItems: number;
}

async function readJson<T>(response: Response): Promise<T> {
  return (await response.json()) as T;
}

export async function getTasks(filters: {
  query?: string;
  status?: TaskStatus | "";
  priority?: TaskPriority | "";
  category?: TaskCategory | "";
  applicationId?: string;
  dueFrom?: string;
  dueTo?: string;
  overdue?: boolean;
  page?: number;
  size?: number;
  sort?: TaskSort;
  direction?: "ASC" | "DESC";
} = {}): Promise<TaskPage> {
  const params = new URLSearchParams();
  if (filters.query?.trim()) params.set("q", filters.query.trim());
  if (filters.status) params.set("status", filters.status);
  if (filters.priority) params.set("priority", filters.priority);
  if (filters.category) params.set("category", filters.category);
  if (filters.applicationId) params.set("applicationId", filters.applicationId);
  if (filters.dueFrom) params.set("dueFrom", filters.dueFrom);
  if (filters.dueTo) params.set("dueTo", filters.dueTo);
  if (filters.overdue) params.set("overdue", "true");
  if (filters.page !== undefined) params.set("page", String(filters.page));
  if (filters.size !== undefined) params.set("size", String(filters.size));
  if (filters.sort) params.set("sort", filters.sort);
  if (filters.direction) params.set("direction", filters.direction);
  const suffix = params.size ? `?${params.toString()}` : "";
  return readJson<TaskPage>(await apiRequest(`tasks${suffix}`));
}

export async function getTask(id: string): Promise<Task> {
  return readJson<Task>(await apiRequest(`tasks/${encodeURIComponent(id)}`));
}

export async function createTask(input: TaskInput): Promise<Task> {
  return readJson<Task>(await apiRequest("tasks", { method: "POST", body: input }));
}

export async function updateTask(id: string, input: TaskInput): Promise<Task> {
  return readJson<Task>(
    await apiRequest(`tasks/${encodeURIComponent(id)}`, { method: "PUT", body: input }),
  );
}

export async function deleteTask(id: string): Promise<void> {
  await apiRequest(`tasks/${encodeURIComponent(id)}`, { method: "DELETE" });
}
