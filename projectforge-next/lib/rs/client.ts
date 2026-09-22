import type {
  DynamicPageResponse,
  FilterFavoritesResponse,
  InitialListData,
  ListMetaData,
  ListPageRequest,
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

// --- CSRF token ---

/**
 * CSRF token of the session, sent with every state-changing call (see RestCsrfProtection).
 *
 * Kept in a module variable rather than localStorage on purpose: an XSS must not be able to read it,
 * and a reload fetches userStatus anyway. The UILayout pages have their own per-page token inside
 * `serverData` (see components/dynamic/) and don't use this one.
 */
let csrfToken: string | null = null;

export function setCsrfToken(token: string | null | undefined): void {
  csrfToken = token ?? null;
}

/** For the one caller that cannot go through rawRequest: the XHR upload in ./upload.ts. */
export function getCsrfToken(): string | null {
  return csrfToken;
}

/** Body Spring sends (403) when the CSRF token was missing or stale (RestCsrfProtection.deny). */
interface CsrfRequiredBody {
  csrfTokenRequired?: boolean;
}

/**
 * Re-fetches the session's token. Called once after a 403 with `csrfTokenRequired`: the token rotates
 * when the session changes (login, session timeout), and the caller shouldn't have to care.
 */
async function refreshCsrfToken(): Promise<boolean> {
  try {
    // fetchUserStatus stores the token itself.
    return !!(await fetchUserStatus()).csrfToken;
  } catch {
    return false;
  }
}

/** GET/HEAD are not checked for a token, mirroring RestCsrfProtection.isStateChangingMethod. */
function isStateChangingMethod(method?: string): boolean {
  const upper = (method ?? "GET").toUpperCase();
  return upper !== "GET" && upper !== "HEAD" && upper !== "OPTIONS";
}

/** Internal: which one-shot recoveries a request has already used up. */
interface RetriesUsed {
  twoFactor?: boolean;
  csrf?: boolean;
}

/**
 * Sends a request and hands back the raw Response, 2FA and CSRF retry included.
 *
 * Dynamic pages need this instead of request(): their protocol uses the status code (406 carries
 * validation errors) and the content type (octet-stream is a download) as data.
 */
export async function rawRequest(
  path: string,
  init: RequestInit,
  signal?: AbortSignal,
  // Internal: each recovery may only be attempted once per call, or a permanently
  // failing check would loop.
  retriesUsed: RetriesUsed = {}
): Promise<Response> {
  // Backend paths (/rs, /rsPublic) are root-relative, NOT prefixed with the
  // app's basePath: Spring serves them at the origin root, not under /next.
  // Dev: next.config.ts rewrites proxy them to :8080. Prod: same Spring origin.
  const url = path;
  const res = await fetch(url, {
    credentials: "include",
    ...init,
    signal,
    headers: {
      // A multipart body (file upload) must keep the Content-Type the browser builds: it carries
      // the boundary, which we cannot know here. Setting it would make Spring's part parser fail.
      ...(init.body instanceof FormData
        ? {}
        : { "Content-Type": "application/json" }),
      // Tells the backend to answer with plain JSON instead of a UILayout
      // ResponseAction (RestAuthenticationUtils.isNextClient).
      "X-PF-Frontend": "next",
      // Central CSRF protection: no call site can forget it. Only sent where the
      // backend checks it, so a stale token can't break a read.
      ...(csrfToken && isStateChangingMethod(init.method)
        ? { "X-PF-CSRF-Token": csrfToken }
        : {}),
      ...(init.headers ?? {}),
    },
  });
  if (res.status === 403) {
    const body = (await res
      .clone()
      .json()
      .catch(() => null)) as (TwoFactorRequiredBody & CsrfRequiredBody) | null;
    if (body?.twoFactorRequired && !retriesUsed.twoFactor && twoFactorHandler) {
      if (await twoFactorHandler(body.expiryMillis)) {
        return rawRequest(path, init, signal, {
          ...retriesUsed,
          twoFactor: true,
        });
      }
    }
    // Stale or missing token (e.g. the session was renewed): fetch a fresh one and repeat once.
    if (body?.csrfTokenRequired && !retriesUsed.csrf) {
      if (await refreshCsrfToken()) {
        return rawRequest(path, init, signal, { ...retriesUsed, csrf: true });
      }
    }
  }
  return res;
}

export async function request<O>(
  path: string,
  init: RequestInit,
  signal?: AbortSignal
): Promise<O> {
  const res = await rawRequest(path, init, signal);
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

/**
 * One server-side page of a list: the backend materializes the ordered id list once per (user, filter)
 * in the session, then serves `[offset, offset+limit)` of it (see `AbstractEntityRest.listPage`). Unlike
 * {@link fetchList} — which returns the whole result set for the client to page — this returns just the
 * page, and `ResultSet.totalSize` carries the size of the whole result.
 *
 * @param refresh Drop the cached id list and rebuild it, for the reload right after the client's own
 *   write; a stale list can only yield a short page, never a forbidden row (access is re-checked per row).
 */
export function fetchListPage<O>(
  entity: string,
  filter: MagicFilter,
  offset: number,
  limit: number,
  refresh = false,
  signal?: AbortSignal,
  doNotStore = false
): Promise<ResultSet<O>> {
  const body: ListPageRequest = { filter, offset, limit, refresh, doNotStore };
  return request<ResultSet<O>>(
    `/rs/${entity}/listPage`,
    { method: "POST", body: JSON.stringify(body) },
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

/**
 * The entity an "add" starts from, as the backend presets it: `OrderEntityRest.newBaseDTO` puts today
 * into the date of entry and the date of offer of a new order (but not into the date of decision, which
 * is only known once the customer has decided), the logged-in project manager in as contact person and
 * `IN_ERSTELLUNG` as its status — and `AuftragDao` refuses to save an order without one.
 *
 * `{entity}/newEntry` answers with that preset and nothing else
 * (`AbstractEntityRest.getNewEntry`). Its predecessor `{entity}/edit` wrapped it in the
 * server-rendered edit layout, which a hand-built page has no use for.
 *
 * @param params Query parameters for the preset. `newBaseDO` is handed the request, so an entity may
 *   read one: `TaskPagesRest` takes `parentTaskId` and presets the parent of a new subtask, which is
 *   the only way the tree can say "add below *this* task". Null and undefined are dropped, so a
 *   caller may pass an unresolved id without special-casing it.
 */
export function fetchNew<O>(
  entity: string,
  params?: Record<string, string | number | boolean | null | undefined>,
  signal?: AbortSignal
): Promise<O> {
  const query = new URLSearchParams();
  for (const [key, value] of Object.entries(params ?? {})) {
    if (value !== undefined && value !== null) query.set(key, String(value));
  }
  const suffix = query.size > 0 ? `?${query}` : "";
  return request<O>(
    `/rs/${entity}/newEntry${suffix}`,
    { method: "GET" },
    signal
  );
}

// Entity writes (saveorupdate, markAsDeleted, …) live in ./entity.ts: they speak the
// ResponseAction protocol, where 406 carries the validation errors, so they need the raw
// Response rather than request()'s parsed body.

// The change history of an entity lives in ./history.ts.

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

/**
 * Also refreshes the CSRF token: this is the one call every app start makes, and the token belongs to
 * the same session as the user it returns. Doing it here means no caller has to remember to.
 */
export async function fetchUserStatus(
  signal?: AbortSignal
): Promise<UserStatus> {
  const status = await request<UserStatus>(
    "/rs/userStatus",
    { method: "GET" },
    signal
  );
  setCsrfToken(status.csrfToken);
  return status;
}

export async function logout(signal?: AbortSignal): Promise<unknown> {
  try {
    return await request<unknown>("/rs/logout", { method: "GET" }, signal);
  } finally {
    // The token belonged to the session that just ended; the next login fetches a new one.
    setCsrfToken(null);
  }
}

// --- Saved list filters (AbstractPagesRest "filter/*", stored in the user's prefs) ---

/**
 * Applies a saved filter and returns the list page state for it.
 *
 * The endpoint also makes it the user's current filter server-side, so the answer is the same payload
 * the page loaded initially: [ListMetaData] for a hand built page, `InitialListData` for one rendered
 * from a `UILayout` (see `AbstractPagesRest.selectFavoriteFilter`). Only the fields both share are
 * read here.
 */
export function selectFilterFavorite(
  entity: string,
  id: number,
  signal?: AbortSignal
): Promise<FilterFavoritesResponse> {
  return request<FilterFavoritesResponse>(
    `/rs/${entity}/filter/select?id=${id}`,
    { method: "GET" },
    signal
  );
}

/** Saves the given filter under a new name. `filter.id` must be unset. */
export function createFilterFavorite(
  entity: string,
  filter: MagicFilter,
  signal?: AbortSignal
): Promise<FilterFavoritesResponse> {
  return request<FilterFavoritesResponse>(
    `/rs/${entity}/filter/create`,
    { method: "POST", body: JSON.stringify(filter) },
    signal
  );
}

/** Overwrites the saved filter identified by `filter.id` with the given values. */
export function updateFilterFavorite(
  entity: string,
  filter: MagicFilter,
  signal?: AbortSignal
): Promise<FilterFavoritesResponse> {
  return request<FilterFavoritesResponse>(
    `/rs/${entity}/filter/update`,
    { method: "POST", body: JSON.stringify(filter) },
    signal
  );
}

export function renameFilterFavorite(
  entity: string,
  id: number,
  newName: string,
  signal?: AbortSignal
): Promise<FilterFavoritesResponse> {
  return request<FilterFavoritesResponse>(
    `/rs/${entity}/filter/rename?id=${id}&newName=${encodeURIComponent(newName)}`,
    { method: "GET" },
    signal
  );
}

/** GET despite deleting — the endpoint is shared with the legacy frontend. */
export function deleteFilterFavorite(
  entity: string,
  id: number,
  signal?: AbortSignal
): Promise<FilterFavoritesResponse> {
  return request<FilterFavoritesResponse>(
    `/rs/${entity}/filter/delete?id=${id}`,
    { method: "GET" },
    signal
  );
}

// --- Menu ---

export function fetchMenu(signal?: AbortSignal): Promise<MenuData> {
  return request<MenuData>("/rs/menu", { method: "GET" }, signal);
}

/**
 * Reports a menu entry the user just opened, so `MenuData.recentMenu` offers it again. All three
 * frontends do this; the server owns the history (RecentMenuEntriesService).
 *
 * @param key `MenuItem.key` as this client received it — the server normalizes and ignores anything
 * that is no menu entry, so a caller needs to know none of those rules.
 */
export async function reportMenuUsage(key: string): Promise<void> {
  try {
    // `keepalive`, because most menu entries still point at Wicket or the legacy app: the document
    // is being torn down while this is in flight, which would cancel an ordinary fetch.
    // The answer is 204, so there is no body to read.
    await rawRequest("/rs/menu/recent", {
      method: "POST",
      body: JSON.stringify({ key }),
      keepalive: true,
    });
  } catch {
    // A history is worth no interruption of the navigation the user asked for.
  }
}

// --- Dynamic Pages ---

export function fetchInitialList(
  category: string,
  signal?: AbortSignal
): Promise<InitialListData> {
  return request<InitialListData>(
    `/rs/${category}/initialList`,
    { method: "GET" },
    signal
  );
}

/**
 * Everything a hand built list page needs beside its rows: the filter fields of the entity, the filter
 * the user left the page with, and their saved filters (`AbstractEntityRest.requestListMeta`).
 *
 * The layout free counterpart of {@link fetchInitialList}, which additionally builds the whole page
 * layout and — for a client that fetches the rows itself — the whole result set.
 */
export function fetchListMeta(
  entity: string,
  signal?: AbortSignal
): Promise<ListMetaData> {
  return request<ListMetaData>(
    `/rs/${entity}/listMeta`,
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

/**
 * @param search extra query parameters to forward verbatim (without `id`), e.g. the `startDate`/
 *   `endDate`/`firstHour` a calendar hands a new timesheet: `TimesheetPagesRest` reads them from
 *   the request to preset the times. `id` travels as its own argument, so it never belongs here.
 */
export function fetchDynamic(
  category: string,
  type?: string,
  id?: string | number,
  search?: string,
  signal?: AbortSignal
): Promise<DynamicPageResponse> {
  const path = type ? `/rs/${category}/${type}` : `/rs/${category}/dynamic`;
  const query = new URLSearchParams(search ?? "");
  if (id != null) query.set("id", String(id));
  const suffix = query.toString();
  return request<DynamicPageResponse>(
    `${path}${suffix ? `?${suffix}` : ""}`,
    { method: "GET" },
    signal
  );
}

// Actions of a dynamic page are sent by lib/rs/dynamic.ts - they need the raw Response.
