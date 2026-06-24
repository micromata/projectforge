import type {
  DynamicPageResponse,
  MagicFilter,
  MenuData,
  ResultSet,
  SystemStatus,
  UserStatus,
} from "./types";

export class RsError extends Error {
  constructor(
    public status: number,
    message: string
  ) {
    super(message);
    this.name = "RsError";
  }
}

async function request<O>(
  path: string,
  init: RequestInit,
  signal?: AbortSignal
): Promise<O> {
  const url = path.startsWith("http") ? path : `/react${path}`;
  const res = await fetch(url, {
    credentials: "include",
    ...init,
    signal,
    headers: {
      "Content-Type": "application/json",
      ...(init.headers ?? {}),
    },
  });
  if (!res.ok) {
    throw new RsError(res.status, `${res.status} ${res.statusText}: ${path}`);
  }
  return (await res.json()) as O;
}

// Same-origin POST so the Next.js mock route handler at /rs/{entity}/list
// can intercept it. When the real Spring backend is wired in, swap this
// for a rewrite in next.config.ts rather than changing call sites.
export function fetchList<O>(
  entity: string,
  filter: MagicFilter,
  signal?: AbortSignal
): Promise<ResultSet<O>> {
  return request<ResultSet<O>>(
    `/rs/${entity}/list`,
    { method: "POST", body: JSON.stringify(filter) },
    signal
  );
}

export function fetchOne<O>(
  entity: string,
  id: number,
  signal?: AbortSignal
): Promise<O> {
  return request<O>(`/rs/${entity}/${id}`, { method: "GET" }, signal);
}

export function save<I, O>(
  entity: string,
  id: number,
  body: I,
  signal?: AbortSignal
): Promise<O> {
  return request<O>(
    `/rs/${entity}/${id}`,
    { method: "PUT", body: JSON.stringify(body) },
    signal
  );
}

export function fetchHistory<O>(
  entity: string,
  id: number,
  signal?: AbortSignal
): Promise<O[]> {
  return request<O[]>(`/rs/${entity}/history/${id}`, { method: "GET" }, signal);
}

// --- Authentication ---

export function fetchSystemStatus(
  signal?: AbortSignal
): Promise<SystemStatus> {
  return request<SystemStatus>(
    "/rsPublic/systemStatus",
    { method: "GET" },
    signal
  );
}

export function login(
  username: string,
  password: string,
  stayLoggedIn: boolean,
  signal?: AbortSignal
): Promise<unknown> {
  return request<unknown>(
    "/rsPublic/login",
    {
      method: "POST",
      body: JSON.stringify({ data: { username, password, stayLoggedIn } }),
    },
    signal
  );
}

export function fetchUserStatus(signal?: AbortSignal): Promise<UserStatus> {
  return request<UserStatus>("/rs/userStatus", { method: "GET" }, signal);
}

export function logout(signal?: AbortSignal): Promise<unknown> {
  return request<unknown>("/rs/logout", { method: "GET" }, signal);
}

// --- 2FA ---

export interface LoginResponse {
  targetType: "CHECK_AUTHENTICATION" | "UPDATE";
  url?: string | null;
  variables?: Record<string, unknown>;
}

export async function loginWithResponse(
  username: string,
  password: string,
  stayLoggedIn: boolean,
  signal?: AbortSignal
): Promise<LoginResponse> {
  const url = `/react/rsPublic/login`;
  const res = await fetch(url, {
    credentials: "include",
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ data: { username, password, stayLoggedIn } }),
    signal,
  });
  const body = await res.json();
  if (!res.ok && body?.targetType !== "UPDATE") {
    throw new RsError(res.status, `${res.status} Login failed`);
  }
  return body as LoginResponse;
}

export async function checkOtp(
  code: string,
  signal?: AbortSignal
): Promise<LoginResponse> {
  const url = `/react/rsPublic/2FA/checkOTP`;
  const res = await fetch(url, {
    credentials: "include",
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ data: { code } }),
    signal,
  });
  const body = await res.json();
  return body as LoginResponse;
}

export function sendSmsCode(signal?: AbortSignal): Promise<unknown> {
  return request<unknown>("/rsPublic/2FA/sendSmsCode", { method: "GET" }, signal);
}

export function sendMailCode(signal?: AbortSignal): Promise<unknown> {
  return request<unknown>("/rsPublic/2FA/sendMailCode", { method: "GET" }, signal);
}

export function cancel2FA(signal?: AbortSignal): Promise<unknown> {
  return request<unknown>("/rsPublic/2FA/cancel", { method: "GET" }, signal);
}

// --- Menu ---

export function fetchMenu(signal?: AbortSignal): Promise<MenuData> {
  return request<MenuData>("/rs/menu", { method: "GET" }, signal);
}

// --- Dynamic Pages ---

export function fetchInitialList(
  category: string,
  signal?: AbortSignal
): Promise<DynamicPageResponse> {
  return request<DynamicPageResponse>(
    `/rs/${category}/initialList`,
    { method: "GET" },
    signal
  );
}

export function fetchListData(
  category: string,
  filter: MagicFilter,
  signal?: AbortSignal
): Promise<DynamicPageResponse> {
  return request<DynamicPageResponse>(
    `/rs/${category}/list`,
    { method: "POST", body: JSON.stringify(filter) },
    signal
  );
}

export function fetchDynamic(
  category: string,
  type?: string,
  id?: string | number,
  signal?: AbortSignal
): Promise<DynamicPageResponse> {
  const path = type ? `/rs/${category}/${type}` : `/rs/${category}/dynamic`;
  const params = id != null ? `?id=${id}` : "";
  return request<DynamicPageResponse>(
    `${path}${params}`,
    { method: "GET" },
    signal
  );
}

export function callAction(
  url: string,
  data: Record<string, unknown>,
  serverData?: Record<string, unknown>,
  signal?: AbortSignal
): Promise<DynamicPageResponse> {
  return request<DynamicPageResponse>(
    url,
    { method: "POST", body: JSON.stringify({ data, serverData }) },
    signal
  );
}
