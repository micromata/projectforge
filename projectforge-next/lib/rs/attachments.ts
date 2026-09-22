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
import { uploadWithProgress, type UploadOptions } from "./upload";

/**
 * `AttachmentsService.DEFAULT_NODE` — the sub-path a single attachment list stores under. Pages
 * with several lists pass their own; books has just the one.
 */
export const DEFAULT_LIST_ID = "attachments";

/**
 * `org.projectforge.common.ZipMode`. Its i18n key is `attachment.zip.<key>` — spelled out in
 * [zipModeMessageKey], since the enum's own `key` (`encrytpedAes256`, typo included) is not
 * derivable from the constant name.
 */
export type ZipMode =
  | "STANDARD"
  | "ENCRYPTED"
  | "ENCRYPTED_STANDARD"
  | "ENCRYPTED_AES128"
  | "ENCRYPTED_AES256";

const ZIP_MODE_KEYS: Record<ZipMode, string> = {
  STANDARD: "standard",
  ENCRYPTED: "encrypted",
  ENCRYPTED_STANDARD: "encryptedStandard",
  ENCRYPTED_AES128: "encrytpedAes128",
  ENCRYPTED_AES256: "encrytpedAes256",
};

/**
 * The message key describing how an attachment is encrypted, or null for a file that isn't.
 *
 * The backend puts this text into `info.encryptionStatus` when it builds the legacy layout, not
 * into the attachment itself (`zipMode` is all that travels), so it is translated here instead.
 */
export function zipModeMessageKey(zipMode: ZipMode | null | undefined) {
  return zipMode ? `attachment.zip.${ZIP_MODE_KEYS[zipMode]}` : null;
}

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
  /** SHA-256 of the file, as the backend computed it on upload. */
  checksum?: string | null;
  /**
   * How the file is encrypted in the storage, `org.projectforge.common.ZipMode` by its enum name.
   * Absent for a plain upload — the field is only set once ProjectForge encrypted the file itself.
   */
  zipMode?: ZipMode | null;
  /** True if the file is encrypted in the storage (zip or AES) and needs a password to open. */
  encrypted?: boolean | null;
  /** True if the user may neither rename nor delete this attachment. */
  readonly?: boolean | null;
  /**
   * Client side only, never sent: this file's description is not the user's to type — it marks the role
   * the file plays for its entity (the invoice PDF's `__INVOICE_PDF__`). So the row offers no rename,
   * which would replace the marker with whatever is displayed in its place, but deletes like any other
   * file (see AttachmentList's `lockedDescription`).
   */
  renameLocked?: boolean;
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
 * Uploads one file, reporting how much of it has gone out.
 *
 * Several files mean several calls — the endpoint takes a single `file` part
 * (`AttachmentsServicesRest.uploadAttachment`), and doing them one at a time is also what keeps
 * the per-file rejection above assignable to a file.
 *
 * The only call in this module that does **not** go through `rawRequest`: progress needs
 * `XMLHttpRequest`, which `fetch` has no equivalent for (see ./upload.ts).
 */
export async function uploadAttachment(
  entity: string,
  id: number,
  file: File,
  listId: string = DEFAULT_LIST_ID,
  options: UploadOptions = {}
): Promise<AttachmentWriteResult> {
  const body = new FormData();
  body.append("file", file);
  const path = `${BASE}/upload/${entity}/${id}/${listId}`;
  const res = await uploadWithProgress(path, body, options);
  if (res.status < 200 || res.status >= 300) {
    throw new RsError(res.status, `${res.status}: ${path}`);
  }
  return interpretWriteBody(res.text, res.status, path);
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
 * Deletes several attachments in one call (`multiDelete`).
 *
 * Not a loop over [deleteAttachment]: every single delete rewrites the entity's JCR node and
 * answers with a full list, so n calls would mean n round trips and n snapshots racing each other
 * in the cache. The backend deletes what the user may delete and answers with the one list that
 * remains.
 */
export function deleteAttachments(
  entity: string,
  id: number,
  fileIds: string[],
  listId: string = DEFAULT_LIST_ID,
  signal?: AbortSignal
): Promise<AttachmentWriteResult> {
  return write(
    `${BASE}/multiDelete`,
    {
      method: "POST",
      body: JSON.stringify({
        data: { category: entity, id, fileIds, listId },
      }),
    },
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

/**
 * Where the given attachments can be downloaded from, as one ZIP (`multiDownload`).
 *
 * A plain URL for the same reason as [attachmentDownloadUrl]. The ids go out in full, although the
 * backend matches them by prefix and the legacy page shortened them to four characters to keep the
 * URL short: 20-character ids (`OakStorage.createRandomId`) stay well inside any URL limit for the
 * handful of files an entity has, and a prefix can match a second file.
 */
export function attachmentsDownloadUrl(
  entity: string,
  id: number,
  fileIds: string[],
  listId: string = DEFAULT_LIST_ID
): string {
  const params = new URLSearchParams({ fileIds: fileIds.join(","), listId });
  return `${BASE}/multiDownload/${entity}/${id}?${params}`;
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
  return interpretWriteBody(await res.text(), res.status, path);
}

/**
 * What a write's body means: the new list, or the refusal it carries.
 *
 * Exported for its unit test only — every caller goes through the functions above.
 *
 * @throws RsError if the body is not a `ResponseAction`. Every write of this module answers with
 * one (`AttachmentsActionListener.afterUpload`/`afterModification`/`afterDeletion`), so anything
 * else did not come from ProjectForge: a proxy's HTML error page, or an empty body from a
 * connection that was cut. Nothing can be concluded from that — least of all success. Reading it
 * as an empty "ok" is how an upload that never arrived used to remove its own progress row
 * without a word.
 */
export function interpretWriteBody(
  text: string,
  status: number,
  path: string
): AttachmentWriteResult {
  const action = parseAction(text);
  if (!action) {
    throw new RsError(
      status,
      `${status}: unexpected response body for ${path}`
    );
  }
  // A refusal comes back as HTTP 200 with a TOAST (see the module comment). Treating it as
  // success would drop a file silently and leave a stale list on screen. But a TOAST is not a
  // refusal by itself — `AttachmentsServicesRest.testDecryption` reports its *success* with one
  // (color "success"), so the colour is what distinguishes them, not the target type.
  if (action.targetType === "TOAST" && action.message?.color !== "success") {
    return {
      kind: "rejected",
      message: action.message?.message ?? "",
    };
  }
  return { kind: "ok", attachments: readAttachments(action) };
}

function parseAction(text: string): ResponseAction | null {
  try {
    const action = JSON.parse(text) as unknown;
    // `{}`, `null` and a bare string all parse; only an object with a target type is an action.
    return action && typeof action === "object" && "targetType" in action
      ? (action as ResponseAction)
      : null;
  } catch {
    return null;
  }
}

/** Unwraps `ResponseAction.variables.data.attachments`, which every write answers with. */
function readAttachments(action: ResponseAction | null): Attachment[] {
  const data = action?.variables?.data as
    | { attachments?: Attachment[] | null }
    | undefined;
  return data?.attachments ?? [];
}
