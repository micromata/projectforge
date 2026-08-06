/**
 * Rest calls of the dynamic (UILayout) pages.
 *
 * These differ from the plain calls in client.ts in three ways, all dictated by the backend:
 * the http method comes from the `ResponseAction`, HTTP 406 is a normal answer carrying
 * `validationErrors`, and the same endpoint may answer with a file instead of JSON.
 */

import { parseContentDispositionFilename } from "@/lib/dynamic/content-disposition";
import { resolveRestUrl } from "@/lib/dynamic/response-action";
import { rawRequest, RsError } from "./client";
import type {
  DynamicPageResponse,
  PostData,
  ResponseAction,
  ServerData,
  ValidationError,
} from "./types";

/** HTTP status Spring answers with when the entity failed validation (see AbstractPagesRest). */
export const NOT_ACCEPTABLE = 406;

export type DynamicMethod = "GET" | "POST" | "PUT" | "DELETE";

export type ActionResult =
  /** The backend answered with a (possibly enveloped) ResponseAction to interpret. */
  | { kind: "action"; response: DynamicPageResponse & ResponseAction }
  /** HTTP 406: the entity was rejected, the errors belong on the fields. */
  | { kind: "validationErrors"; validationErrors: ValidationError[] }
  /** The response body was a file; it has already been handed to the browser. */
  | { kind: "download" };

/**
 * Calls an action url of a dynamic page and classifies the answer.
 *
 * GET is sent without a body, mirroring the legacy `callAction`: the backend's GET endpoints take
 * their arguments from the query string, and Spring rejects a body on some of them.
 */
export async function callDynamicAction(
  method: DynamicMethod,
  url: string,
  postData: PostData,
  signal?: AbortSignal
): Promise<ActionResult> {
  const res = await rawRequest(
    resolveRestUrl(url),
    {
      method,
      body: method === "GET" ? undefined : JSON.stringify(postData),
    },
    signal
  );

  const contentType = res.headers.get("Content-Type") ?? "";

  if (res.status === NOT_ACCEPTABLE) {
    const body = (await res.json().catch(() => null)) as {
      validationErrors?: ValidationError[];
    } | null;
    return {
      kind: "validationErrors",
      validationErrors: body?.validationErrors ?? [],
    };
  }

  if (!res.ok) {
    throw new RsError(res.status, `${res.status} ${res.statusText}: ${url}`);
  }

  if (contentType.includes("application/json")) {
    return {
      kind: "action",
      response: (await res.json()) as DynamicPageResponse & ResponseAction,
    };
  }

  if (contentType.includes("application/octet-stream")) {
    saveBlob(
      await res.blob(),
      parseContentDispositionFilename(res.headers.get("Content-Disposition"))
    );
    return { kind: "download" };
  }

  // An empty 200 (e.g. a plain "OK") means there is nothing left to do.
  return {
    kind: "action",
    response: { targetType: "NOTHING" } as DynamicPageResponse,
  };
}

/**
 * Notifies the backend that a watched field changed, so it can recalculate dependent fields or
 * swap parts of the layout. See AbstractPagesRest.watchFields / UILayout.watchFields.
 */
export function postWatchFields(
  category: string,
  postData: PostData,
  signal?: AbortSignal
): Promise<ActionResult> {
  return callDynamicAction("POST", `${category}/watchFields`, postData, signal);
}

/**
 * Fetches the suggestions of an autocompletion url.
 *
 * The url comes from the layout with a literal `:search` placeholder (see
 * AutoCompletion.getAutoCompletionUrl); the response is either a list of plain strings
 * (`{category}/autocomplete?property=…`) or a list of `DisplayObject`s (`{category}/autosearch`).
 */
export async function fetchAutoCompletion<T = string>(
  url: string,
  search: string,
  params?: Record<string, unknown>,
  signal?: AbortSignal
): Promise<T[]> {
  let path = resolveRestUrl(url).replace(":search", encodeURIComponent(search));
  const extra = Object.entries(params ?? {}).filter(
    ([, value]) => value != null
  );
  if (extra.length > 0) {
    const query = extra
      .map(([key, value]) => `${key}=${encodeURIComponent(String(value))}`)
      .join("&");
    path += `${path.includes("?") ? "&" : "?"}${query}`;
  }
  const res = await rawRequest(path, { method: "GET" }, signal);
  if (!res.ok) {
    throw new RsError(res.status, `${res.status} ${res.statusText}: ${url}`);
  }
  return (await res.json()) as T[];
}

/** Builds the request body every mutating call of a dynamic page expects (rest/dto/PostData.kt). */
export function buildPostData(
  data: Record<string, unknown>,
  serverData?: ServerData,
  watchFieldsTriggered?: string[]
): PostData {
  return {
    data,
    ...(serverData ? { serverData } : {}),
    ...(watchFieldsTriggered?.length ? { watchFieldsTriggered } : {}),
  };
}

/**
 * Hands a blob to the browser as a download. Replaces the `js-file-download` dependency of the
 * legacy app: an object url on a synthetic anchor does the same in a few lines.
 */
function saveBlob(blob: Blob, filename: string): void {
  const objectUrl = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = objectUrl;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  // Chrome needs the url alive until the click was processed.
  window.setTimeout(() => URL.revokeObjectURL(objectUrl), 0);
}
