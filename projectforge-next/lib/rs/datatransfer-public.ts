/**
 * The external access of a data transfer area (`DataTransferPublicNextRest`): someone without a
 * ProjectForge account opens the area's link, logs in with its password and sees, uploads or
 * downloads its files — whatever the area allows.
 *
 * The login lives in the http session (`DataTransferPublicSession`), so no token travels after it.
 * The file operations are those of the attachment components, pointed at the public endpoints by
 * [publicAttachmentService].
 */

import { rawRequest, request, RsError } from "./client";
import type { Attachment, AttachmentService } from "./attachments";

const BASE = "/rsPublic/datatransfer";
const PATH = `${BASE}/next`;

/** The attachment category of the areas (`DataTransferPlugin.ID`). */
export const PUBLIC_DATATRANSFER_ENTITY = "datatransfer";

/** `DataTransferPublicNextRest.State`. Without a login only `loggedIn` and `areaName` are set. */
export interface DataTransferPublicState {
  loggedIn: boolean;
  /** The area's name, or the generic heading while not logged in. */
  areaName?: string | null;
  id?: number | null;
  description?: string | null;
  externalDownloadEnabled?: boolean | null;
  externalUploadEnabled?: boolean | null;
  maxUploadSizeFormatted?: string | null;
  /** All files as one ZIP, offered up to a gigabyte in total. */
  downloadAllAvailable?: boolean;
  userInfo?: string | null;
  /** Without download access only the files uploaded in this session. */
  attachments?: Attachment[] | null;
}

export type DataTransferPublicLoginResult =
  | { kind: "ok"; state: DataTransferPublicState }
  /** Wrong password, unknown link, or locked for a while after failed attempts — already translated. */
  | { kind: "failed"; message: string };

export function dataTransferPublicStateQueryKey(token: string) {
  return ["dataTransferPublic", token] as const;
}

export function fetchDataTransferPublicState(
  token: string,
  signal?: AbortSignal
): Promise<DataTransferPublicState> {
  const params = new URLSearchParams({ token });
  return request<DataTransferPublicState>(
    `${PATH}/state?${params}`,
    { method: "GET" },
    signal
  );
}

/** A refused login is HTTP 400 with the reason; anything else unexpected throws. */
export async function loginDataTransferPublic(
  token: string,
  password: string,
  userInfo: string,
  signal?: AbortSignal
): Promise<DataTransferPublicLoginResult> {
  const path = `${PATH}/login`;
  const res = await rawRequest(
    path,
    {
      method: "POST",
      body: JSON.stringify({ token, password, userInfo }),
    },
    signal
  );
  if (res.status === 400) {
    const body = (await res.json().catch(() => null)) as {
      message?: string;
    } | null;
    return { kind: "failed", message: body?.message ?? "" };
  }
  if (!res.ok) {
    throw new RsError(res.status, `${res.status} ${res.statusText}: ${path}`);
  }
  return { kind: "ok", state: (await res.json()) as DataTransferPublicState };
}

/** Ends the session, and with it the login to every area opened in it. Answers the translated note. */
export async function logoutDataTransferPublic(
  signal?: AbortSignal
): Promise<string> {
  const body = await request<{ message?: string }>(
    `${PATH}/logout`,
    { method: "POST" },
    signal
  );
  return body.message ?? "";
}

/** All files of the area as one ZIP (`DataTransferPublicServicesRest.downloadAll`). */
export function dataTransferPublicDownloadAllUrl(id: number): string {
  return `${BASE}/downloadAll/${PUBLIC_DATATRANSFER_ENTITY}/${id}`;
}

/**
 * The attachment components on the public endpoints. They answer like `AttachmentsServicesRest`, but
 * the list is read from the state (there is no entity to read it from), nothing can be encrypted, and
 * who uploaded a file is not shown — for an external upload that is an ip address.
 */
export function publicAttachmentService(
  token: string,
  multiDownload: boolean
): AttachmentService {
  return {
    base: BASE,
    fetchAttachments: async (_entity, _id, signal) =>
      (await fetchDataTransferPublicState(token, signal)).attachments ?? [],
    encryption: false,
    multiDownload,
    userInfo: false,
  };
}
