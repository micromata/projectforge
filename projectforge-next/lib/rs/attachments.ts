/**
 * Attachments of an entity (`AttachmentsServicesRest`, JCR-backed).
 *
 * The same endpoints the UILayout pages use, so they speak the `PostData`/`ResponseAction`
 * protocol (see ./entity.ts) rather than the plain JSON of client.ts. Two consequences shape
 * this module:
 *
 * - Every write answers with the *complete* new list under `variables.data.attachments`, so a
 *   caller never has to re-read the entity — [readAttachments] unwraps that.
 * - A rejected upload is **HTTP 200** carrying a TOAST action instead of an error (a duplicate
 *   filename, an oversized file — `AttachmentsService`/`GlobalDefaultExceptionHandler` both go
 *   through `UIToast`), so the status code alone cannot tell success from failure.
 *
 * The CSRF token is not passed here: `rawRequest` sets the header for every state changing call.
 */

import { rawRequest, request, RsError } from "./client";
import type { ResponseAction } from "./types";

/**
 * `AttachmentsService.DEFAULT_NODE` — the sub-path a single attachment list stores under. Pages
 * with several lists pass their own; books has just the one.
 */
export const DEFAULT_LIST_ID = "attachments";

/** org.projectforge.framework.jcr.Attachment, as it arrives on the wire. */
export interface Attachment {
  /** Assigned by the JCR; identifies the file in every call below. */
  fileId: string;
  name: string;
  size?: number | null;
  /** Pre-formatted by the backend (`NumberHelper.formatBytes`), e.g. "22bytes". */
  sizeHumanReadable?: string | null;
  description?: string | null;
  created?: string | null;
  createdByUser?: string | null;
  lastUpdate?: string | null;
  lastUpdateByUser?: string | null;
  /** Localised "a few seconds ago", built by the backend. */
  lastUpdateTimeAgo?: string | null;
  /** Formatted by the backend in the user's timezone and date format. */
  createdFormatted?: string | null;
  lastUpdateFormatted?: string | null;
  checksum?: string | null;
  /** True if the file is encrypted in the storage (zip or AES) and needs a password to open. */
  encrypted?: boolean | null;
  /** True if the user may neither rename nor delete this attachment. */
  readonly?: boolean | null;
}

/** What a write returns: the entity's full attachment list, or the reason it was refused. */
export type AttachmentWriteResult =
  | { kind: "ok"; attachments: Attachment[] }
  /** `message` is already translated by the backend and can be shown as it is. */
  | { kind: "rejected"; message: string };

/** Identifies one attachment for modify/delete. */
export interface AttachmentRef {
  entity: string;
  id: number;
  fileId: string;
  listId?: string;
}

const BASE = "/rs/attachments";

/**
 * The current list of an entity's attachments.
 *
 * There is no read endpoint of its own: `AbstractPagesRest` puts them into the entity itself
 * (see its `getItemAndLayout`), so this reads the entity and picks the field out. Kept here so a
 * caller doesn't have to know that, and so the entity's own type stays free of it.
 */
export async function fetchAttachments(
  entity: string,
  id: number,
  signal?: AbortSignal
): Promise<Attachment[]> {
  const dto = await request<{ attachments?: Attachment[] | null }>(
    `/rs/${entity}/${id}`,
    { method: "GET" },
    signal
  );
  return dto.attachments ?? [];
}

/**
 * Uploads one file. Several files mean several calls — the endpoint takes a single `file` part
 * (`AttachmentsServicesRest.uploadAttachment`), and doing them one at a time is also what keeps
 * the per-file rejection above assignable to a file.
 */
export function uploadAttachment(
  entity: string,
  id: number,
  file: File,
  listId: string = DEFAULT_LIST_ID,
  signal?: AbortSignal
): Promise<AttachmentWriteResult> {
  const body = new FormData();
  body.append("file", file);
  // No Content-Type header: the browser has to set it, boundary included (see rawRequest).
  return write(
    `${BASE}/upload/${entity}/${id}/${listId}`,
    { method: "POST", body },
    signal
  );
}

/** Renames an attachment and/or changes its description. Both are sent, so both must be given. */
export function modifyAttachment(
  ref: AttachmentRef,
  name: string,
  description: string,
  signal?: AbortSignal
): Promise<AttachmentWriteResult> {
  return write(
    `${BASE}/modify`,
    {
      method: "POST",
      body: JSON.stringify({
        data: {
          ...refData(ref),
          // `attachment` is the DTO the backend reads name and description off.
          attachment: { fileId: ref.fileId, name, description },
        },
      }),
    },
    signal
  );
}

/** Deletes an attachment. Irreversible — the JCR keeps no history of removed files. */
export function deleteAttachment(
  ref: AttachmentRef,
  signal?: AbortSignal
): Promise<AttachmentWriteResult> {
  return write(
    `${BASE}/delete`,
    { method: "POST", body: JSON.stringify({ data: refData(ref) }) },
    signal
  );
}

/**
 * Where a single attachment can be downloaded from.
 *
 * A plain URL rather than a fetch: the answer is the file itself (`Content-Disposition:
 * attachment`), so the browser has to do the saving. Same-origin and cookie-authenticated, which
 * is why no token is needed — the call is a GET and changes nothing.
 */
export function attachmentDownloadUrl(ref: AttachmentRef): string {
  const params = new URLSearchParams({
    fileId: ref.fileId,
    listId: ref.listId ?? DEFAULT_LIST_ID,
  });
  return `${BASE}/download/${ref.entity}/${ref.id}?${params}`;
}

function refData(ref: AttachmentRef) {
  return {
    // The backend's `category` is the entity's rest path (`AbstractPagesRest.category`).
    category: ref.entity,
    id: ref.id,
    fileId: ref.fileId,
    listId: ref.listId ?? DEFAULT_LIST_ID,
  };
}

async function write(
  path: string,
  init: RequestInit,
  signal?: AbortSignal
): Promise<AttachmentWriteResult> {
  const res = await rawRequest(path, init, signal);
  if (!res.ok) {
    throw new RsError(res.status, `${res.status} ${res.statusText}: ${path}`);
  }
  const action = (await res.json().catch(() => null)) as ResponseAction | null;
  // A refusal comes back as HTTP 200 with a TOAST (see the module comment). Treating it as
  // success would drop a file silently and leave a stale list on screen.
  if (action?.targetType === "TOAST") {
    return {
      kind: "rejected",
      message: action.message?.message ?? "",
    };
  }
  return { kind: "ok", attachments: readAttachments(action) };
}

/** Unwraps `ResponseAction.variables.data.attachments`, which every write answers with. */
function readAttachments(action: ResponseAction | null): Attachment[] {
  const data = action?.variables?.data as
    | { attachments?: Attachment[] | null }
    | undefined;
  return data?.attachments ?? [];
}
