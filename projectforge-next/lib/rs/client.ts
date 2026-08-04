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

// Body Spring sends (403) when My2FARequestHandler demands a second factor for a
// protected action, see RestAuthenticationUtils.doFilter / TwoFactorRequired.
interface TwoFactorRequiredBody {
  twoFactorRequired?: boolean;
  expiryMillis?: number;
}

/**
 * Asks the user for a second factor and resolves to true once it succeeded.
 * Registered by the app shell (see components/shared/two-factor-provider.tsx) so
 * request() can transparently repeat the interrupted call.
 */
export type TwoFactorHandler = (expiryMillis?: number) => Promise<boolean>;

let twoFactorHandler: TwoFactorHandler | null = null;

export function setTwoFactorHandler(handler: TwoFactorHandler | null): void {
  twoFactorHandler = handler;
}

export async function request<O>(
  path: string,
  init: RequestInit,
  signal?: AbortSignal,
  // Internal: a retried request must not trigger the 2FA dialog a second time.
  isRetry = false
): Promise<O> {
  // Backend paths (/rs, /rsPublic) are root-relative, NOT prefixed with the
  // app's basePath: Spring serves them at the origin root, not under /next.
  // Dev: next.config.ts rewrites proxy them to :8080. Prod: same Spring origin.
  const url = path;
  const res = await fetch(url, {
    credentials: "include",
    ...init,
    signal,
    headers: {
      "Content-Type": "application/json",
      // Tells the backend to answer with plain JSON instead of a UILayout
      // ResponseAction (RestAuthenticationUtils.isNextClient).
      "X-PF-Frontend": "next",
      ...(init.headers ?? {}),
    },
  });
  if (res.status === 403 && !isRetry && twoFactorHandler) {
    const body = (await res
      .clone()
      .json()
      .catch(() => null)) as TwoFactorRequiredBody | null;
    if (body?.twoFactorRequired) {
      if (await twoFactorHandler(body.expiryMillis)) {
        return request<O>(path, init, signal, true);
      }
    }
  }
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

export function fetchSystemStatus(signal?: AbortSignal): Promise<SystemStatus> {
  return request<SystemStatus>(
    "/rsPublic/systemStatus",
    { method: "GET" },
    signal
  );
}

// Login, 2FA and password reset live in ./auth.ts (they speak the next-only
// JSON contract, not the UILayout ResponseAction protocol).

export function fetchUserStatus(signal?: AbortSignal): Promise<UserStatus> {
  return request<UserStatus>("/rs/userStatus", { method: "GET" }, signal);
}

export function logout(signal?: AbortSignal): Promise<unknown> {
  return request<unknown>("/rs/logout", { method: "GET" }, signal);
}

// --- Column state (per entity category, stored in the user's prefs) ---

/** TanStack Table's own state shape, mirroring GridState/DataTableStateRequest. */
export interface ColumnStateDto {
  columnOrder?: string[];
  columnSizing?: Record<string, number>;
  columnVisibility?: Record<string, boolean>;
  columnPinning?: { left?: string[]; right?: string[] };
  sorting?: { id: string; desc: boolean }[];
  columnFilters?: { id: string; value: unknown }[];
}

export function fetchColumnStates(
  entity: string,
  signal?: AbortSignal
): Promise<ColumnStateDto> {
  return request<ColumnStateDto>(
    `/rs/${entity}/columnStates`,
    { method: "GET" },
    signal
  );
}

export function saveColumnStates(
  entity: string,
  state: ColumnStateDto,
  signal?: AbortSignal
): Promise<unknown> {
  return request<unknown>(
    `/rs/${entity}/setColumnStates`,
    { method: "POST", body: JSON.stringify(state) },
    signal
  );
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
