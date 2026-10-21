/**
 * Rest calls answering in the shape of the UILayout endpoints (action urls, autocompletion).
 *
 * These differ from the plain calls in client.ts in three ways, all dictated by the backend:
 * HTTP 406 is a normal answer carrying `validationErrors`, the same endpoint may answer with a
 * file instead of JSON, and an url may come from the backend relative to the rest base.
 */

import { parseContentDispositionFilename } from "@/lib/dynamic/content-disposition";
import { resolveRestUrl } from "@/lib/dynamic/response-action";
import { rawRequest, RsError } from "./client";
import { responseBlob, saveBlob } from "./download";
import type {
  MagicFilter,
  PostData,
  ResponseAction,
  ValidationError,
} from "./types";

/** HTTP status Spring answers with when the entity failed validation (see AbstractPagesRest). */
export const NOT_ACCEPTABLE = 406;

type DynamicMethod = "GET" | "POST" | "PUT" | "DELETE";

export type ActionResult =
  /** The backend answered with a ResponseAction (possibly inside a FormLayoutData envelope). */
  | { kind: "action"; response: ResponseAction }
  /** HTTP 406: the entity was rejected, the errors belong on the fields. */
  | { kind: "validationErrors"; validationErrors: ValidationError[] }
  /** The response body was a file; it has already been handed to the browser. */
  | { kind: "download" };

/**
 * Calls an action url and classifies the answer.
 *
 * GET is sent without a body, mirroring the legacy `callAction`: the backend's GET endpoints take
 * their arguments from the query string, and Spring rejects a body on some of them.
 *
 * The body is a `PostData` for the actions of a form, and the bare `MagicFilter` for list actions
 * that take a filter instead.
 */
export async function callDynamicAction(
  method: DynamicMethod,
  url: string,
  body: PostData | MagicFilter,
  signal?: AbortSignal
): Promise<ActionResult> {
  const res = await rawRequest(
    resolveRestUrl(url),
    {
      method,
      body: method === "GET" ? undefined : JSON.stringify(body),
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
      response: (await res.json()) as ResponseAction,
    };
  }

  if (contentType.includes("application/octet-stream")) {
    saveBlob(
      await responseBlob(res),
      parseContentDispositionFilename(res.headers.get("Content-Disposition"))
    );
    return { kind: "download" };
  }

  // An empty 200 (e.g. a plain "OK") means there is nothing left to do.
  return {
    kind: "action",
    response: { targetType: "NOTHING" } as ResponseAction,
  };
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
