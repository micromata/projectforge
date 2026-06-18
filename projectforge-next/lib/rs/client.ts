import type { MagicFilter, ResultSet } from "./types";

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
  const res = await fetch(path, {
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
