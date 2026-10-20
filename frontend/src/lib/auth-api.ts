export interface Account {
  id: string;
  email: string;
  displayName: string;
}

interface CsrfToken {
  headerName: string;
  token: string;
}

interface ApiProblem {
  title?: string;
  detail?: string;
}

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function readCsrfToken(): Promise<CsrfToken> {
  const response = await fetch("/api/v1/auth/csrf", {
    credentials: "same-origin",
    cache: "no-store",
  });
  if (!response.ok) {
    throw new ApiError("We couldn’t securely prepare your request. Please try again.", response.status);
  }
  return (await response.json()) as CsrfToken;
}

export async function apiRequest(
  path: string,
  options: { method?: string; body?: object } = {},
): Promise<Response> {
  const method = options.method ?? "GET";
  const headers: Record<string, string> = {};
  if (options.body) headers["Content-Type"] = "application/json";
  if (method !== "GET" && method !== "HEAD") {
    const csrf = await readCsrfToken();
    headers[csrf.headerName] = csrf.token;
  }
  const response = await fetch(`/api/v1/${path}`, {
    method,
    credentials: "same-origin",
    cache: "no-store",
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined,
  });
  if (!response.ok) {
    const problem = (await response.json().catch(() => ({}))) as ApiProblem;
    throw new ApiError(
      problem.detail ?? problem.title ?? "Something went wrong. Please try again.",
      response.status,
    );
  }
  return response;
}

async function postAuth<T>(path: string, body: object): Promise<T> {
  const response = await apiRequest(`auth/${path}`, { method: "POST", body });
  return (await response.json()) as T;
}

export async function getCurrentAccount(): Promise<Account> {
  const response = await apiRequest("auth/me");
  return (await response.json()) as Account;
}

export async function signOut(): Promise<void> {
  await apiRequest("auth/logout", { method: "POST" });
}

export function registerAccount(input: {
  email: string;
  displayName: string;
  password: string;
}): Promise<Account> {
  return postAuth<Account>("register", input);
}

export function signIn(input: { email: string; password: string }): Promise<Account> {
  return postAuth<Account>("login", input);
}
