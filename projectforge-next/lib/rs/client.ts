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

// Same-origin POST so the Next.js mock route handler at /rs/{entity}/list
// can intercept it. When the real Spring backend is wired in, swap this
// for a rewrite in next.config.ts rather than changing call sites.
export async function fetchList<O>(
  entity: string,
  filter: MagicFilter,
  signal?: AbortSignal
): Promise<ResultSet<O>> {
  const res = await fetch(`/rs/${entity}/list`, {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(filter),
    signal,
  });
  if (!res.ok) {
    throw new RsError(
      res.status,
      `${res.status} ${res.statusText}: /rs/${entity}/list`
    );
  }
  return (await res.json()) as ResultSet<O>;
}
