/**
 * Attachments of an entity (`AttachmentsServicesRest`, backed by the file store).
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
import type { InactiveFlags } from "@/lib/page-def/reference";

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

/** org.projectforge.framework.files.Attachment, as it arrives on the wire. */
/** Whether the creating or last modifying user of an attachment is still active, for inactiveRefClass. */
export function attachmentUserFlags(
  attachment: Attachment,
  key: "createdByUser" | "lastUpdateByUser"
): InactiveFlags {
  return {
    deleted: !!attachment[`${key}Deleted`],
    deactivated: !!attachment[`${key}Deactivated`],
  };
}

export interface Attachment {
  /** Assigned by the file store; identifies the file in every call below. */
  fileId: string;
  name: string;
  size?: number | null;
  /** Pre-formatted by the backend (`NumberHelper.formatBytes`), e.g. "22bytes". */
  sizeHumanReadable?: string | null;
  description?: string | null;
  created?: string | null;
  createdByUser?: string | null;
  /** The creating user is deleted or deactivated: the name is struck through (see inactiveRefClass). */
  createdByUserDeleted?: boolean | null;
  createdByUserDeactivated?: boolean | null;
  lastUpdate?: string | null;
  lastUpdateByUser?: string | null;
  /** As createdByUserDeleted/-Deactivated, for the last modifying user. */
  lastUpdateByUserDeleted?: boolean | null;
  lastUpdateByUserDeactivated?: boolean | null;
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
   * Extra values an entity's backend attaches per file. Only `expiryInfo` is read here: the data
   * transfer areas put the time left until the file is deleted automatically, already translated
   * ("expires in 5 days"), into it (`DataTransferAttachmentsActionListener`).
   */
  info?: { expiryInfo?: string | null } | null;
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
  /** The endpoints to call, see [AttachmentService.base]. Defaults to `AttachmentsServicesRest`. */
  base?: string;
}

const BASE = "/rs/attachments";

/**
 * The backend a list of attachments talks to, and what it offers.
 *
 * `AttachmentsServicesRest` for every entity of a logged-in user ([DEFAULT_ATTACHMENT_SERVICE]). The
 * external access of a data transfer area has endpoints of its own under the same names and
 * signatures (`DataTransferPublicServicesRest`), but no reading via the entity, and — depending on
 * the area — no ZIP download.
 */
export interface AttachmentService {
  /** Base path of the upload, download, modify and delete endpoints. */
  base: string;
  /** Reads the current list (see [fetchAttachments] for the default). */
  fetchAttachments: (
    entity: string,
    id: number,
    signal?: AbortSignal
  ) => Promise<Attachment[]>;
  /** Encrypting a file and testing a password against it are offered. */
  encryption: boolean;
  /** Several files can be downloaded as one ZIP (`multiDownload`). */
  multiDownload: boolean;
  /**
   * The toolbar offers all files as one ZIP, next to the selected ones. Off where the page has a
   * "download all" of its own (the external access of a data transfer area, with a size limit).
   */
  downloadAll: boolean;
  /** Who uploaded or changed a file is shown. */
  userInfo: boolean;
  /**
   * Called when a write (upload, rename, delete, encrypt) failed on the way, not refused by the backend.
   * The external access of a data transfer area re-reads its login state then: the usual cause there is
   * an expired session, which the page should answer with its login form.
   */
  onWriteError?: () => void;
  /**
   * The largest file the backend takes, checked before the transfer so an oversized file isn't sent in
   * vain. `formatted` is the backend's text for it. Unset: no check here, the backend still refuses.
   */
  maxFileSize?: { bytes: number; formatted: string };
  /** The most a ZIP of selected files may contain (as [maxFileSize]); unset: no limit. */
  maxZipSize?: { bytes: number; formatted: string };
}

export const DEFAULT_ATTACHMENT_SERVICE: AttachmentService = {
  base: BASE,
  fetchAttachments,
  encryption: true,
  multiDownload: true,
  downloadAll: true,
  userInfo: true,
};

/**
 * The current list of an entity's attachments.
 *
 * There is no read endpoint of its own: `AbstractEntityRest` puts them into the entity itself
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
  options: UploadOptions = {},
  base: string = BASE
): Promise<AttachmentWriteResult> {
  const body = new FormData();
  body.append("file", file);
  const path = `${base}/upload/${entity}/${id}/${listId}`;
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
    `${ref.base ?? BASE}/modify`,
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

/** Deletes an attachment. Irreversible — the file store keeps no history of removed files. */
export function deleteAttachment(
  ref: AttachmentRef,
  signal?: AbortSignal
): Promise<AttachmentWriteResult> {
  return write(
    `${ref.base ?? BASE}/delete`,
    { method: "POST", body: JSON.stringify({ data: refData(ref) }) },
    signal
  );
}

/**
 * Deletes several attachments in one call (`multiDelete`).
 *
 * Not a loop over [deleteAttachment]: every single delete rewrites the entity's attachments and
 * answers with a full list, so n calls would mean n round trips and n snapshots racing each other
 * in the cache. The backend deletes what the user may delete and answers with the one list that
 * remains.
 */
export function deleteAttachments(
  entity: string,
  id: number,
  fileIds: string[],
  listId: string = DEFAULT_LIST_ID,
  signal?: AbortSignal,
  base: string = BASE
): Promise<AttachmentWriteResult> {
  return write(
    `${base}/multiDelete`,
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
  return `${ref.base ?? BASE}/download/${ref.entity}/${ref.id}?${params}`;
}

/**
 * Where the given attachments can be downloaded from, as one ZIP (`multiDownload`).
 *
 * A plain URL for the same reason as [attachmentDownloadUrl]. The ids go out as [shortenFileIds] makes
 * them: in full for the handful of files an entity usually has, shortened only for a selection that
 * would make the URL too long.
 *
 * @param allFileIds every file of the list, which a shortened id must not match as well.
 */
export function attachmentsDownloadUrl(
  entity: string,
  id: number,
  fileIds: string[],
  listId: string = DEFAULT_LIST_ID,
  base: string = BASE,
  allFileIds: string[] = fileIds
): string {
  const params = new URLSearchParams({
    fileIds: shortenFileIds(fileIds, allFileIds).join(","),
    listId,
  });
  return `${base}/multiDownload/${entity}/${id}?${params}`;
}

/** Up to this length the ids of a ZIP download go out in full. */
const MAX_FILE_IDS_LENGTH = 1500;
/** The shortest prefix sent, as the legacy page did. */
const MIN_FILE_ID_PREFIX = 4;

/**
 * The ids for `multiDownload`, which matches them by prefix. In full as long as they fit into
 * [MAX_FILE_IDS_LENGTH] characters, otherwise each one cut to the shortest prefix (at least
 * [MIN_FILE_ID_PREFIX]) that matches no other file of the list — 20-character ids
 * (`FileStore.createRandomId`) would make a URL with hundreds of files too long for the server.
 *
 * Exported for its unit test.
 */
export function shortenFileIds(
  fileIds: string[],
  allFileIds: string[]
): string[] {
  const fullLength = fileIds.reduce(
    (sum, fileId) => sum + fileId.length + 1,
    0
  );
  if (fullLength <= MAX_FILE_IDS_LENGTH) return fileIds;
  return fileIds.map((fileId) => {
    for (let length = MIN_FILE_ID_PREFIX; length < fileId.length; length++) {
      const prefix = fileId.slice(0, length);
      if (
        !allFileIds.some(
          (other) => other !== fileId && other.startsWith(prefix)
        )
      ) {
        return prefix;
      }
    }
    return fileId;
  });
}

/**
 * The two modes a file can be encrypted with — what the legacy dialog offered
 * (`AttachmentPageRest.addShowEncryptionOption`). The others in [ZipMode] are only ever detected on
 * upload, never chosen.
 */
export type EncryptionMode = Extract<
  ZipMode,
  "ENCRYPTED_STANDARD" | "ENCRYPTED_AES256"
>;

/**
 * The password was refused: too short to encrypt with, or not the one the file was encrypted with.
 * `message` is the backend's, already translated, and belongs to the password field.
 */
export interface PasswordRefused {
  kind: "invalid";
  message: string;
}

/**
 * Replaces the file by a password protected ZIP of it (`AttachmentsServicesRest.encrypt`): `a.pdf`
 * becomes `a.pdf.zip` under a new `fileId`, the original is deleted. The answer is the new list.
 */
export function encryptAttachment(
  ref: AttachmentRef,
  password: string,
  mode: EncryptionMode,
  signal?: AbortSignal
): Promise<AttachmentWriteResult | PasswordRefused> {
  return passwordWrite(
    `${ref.base ?? BASE}/encrypt`,
    ref,
    { password, newZipMode: mode },
    signal
  );
}

/**
 * Checks a password against an encrypted file (`AttachmentsServicesRest.testDecryption`). Changes
 * nothing, so the answer carries no list — `ok` is all there is to it.
 */
export async function testAttachmentDecryption(
  ref: AttachmentRef,
  password: string,
  signal?: AbortSignal
): Promise<
  { kind: "ok" } | { kind: "rejected"; message: string } | PasswordRefused
> {
  const result = await passwordWrite(
    `${ref.base ?? BASE}/testDecryption`,
    ref,
    { password },
    signal
  );
  // The success TOAST has no list; passing on its empty one would read as "all files gone".
  return result.kind === "ok" ? { kind: "ok" } : result;
}

/**
 * Like [write], but HTTP 406 is an answer here, not an error: both password calls refuse a password
 * that way, with a validation error on `attachment.password`.
 */
async function passwordWrite(
  path: string,
  ref: AttachmentRef,
  attachment: { password: string; newZipMode?: EncryptionMode },
  signal?: AbortSignal
): Promise<AttachmentWriteResult | PasswordRefused> {
  const res = await rawRequest(
    path,
    {
      method: "POST",
      body: JSON.stringify({
        data: {
          ...refData(ref),
          attachment: { fileId: ref.fileId, ...attachment },
        },
      }),
    },
    signal
  );
  if (res.status === NOT_ACCEPTABLE) {
    const body = (await res.json().catch(() => null)) as ResponseAction | null;
    return {
      kind: "invalid",
      message: body?.validationErrors?.[0]?.message ?? "",
    };
  }
  if (!res.ok) {
    throw new RsError(res.status, `${res.status} ${res.statusText}: ${path}`);
  }
  return interpretWriteBody(await res.text(), res.status, path);
}

const NOT_ACCEPTABLE = 406;

function refData(ref: AttachmentRef) {
  return {
    // The backend's `category` is the entity's rest path (`AbstractEntityRest.category`).
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
