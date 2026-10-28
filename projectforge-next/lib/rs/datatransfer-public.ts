/**
 * The external access of a data transfer area (`DataTransferPublicNextRest`): someone without a
 * ProjectForge account opens the area's link, logs in with its password and sees, uploads or
 * downloads its files — whatever the area allows.
 *
 * The login lives in the http session (`DataTransferPublicSession`), so no token travels after it.
 * The file operations are those of the attachment components, pointed at the public endpoints by
 * [publicAttachmentService].
 */

import type { QueryClient } from "@tanstack/react-query";
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
  /** The largest file that can still be uploaded, in bytes — the area's limit or what is left of its capacity. */
  maxUploadSize?: number | null;
  maxUploadSizeFormatted?: string | null;
  /** All files as one ZIP, offered up to a gigabyte in total. */
  downloadAllAvailable?: boolean;
  /** The same limit for a ZIP of selected files, in bytes. */
  maxZipSize?: number;
  maxZipSizeFormatted?: string;
  userInfo?: string | null;
  /**
   * Who uploaded and changed the files is shown: the visitor is logged in to ProjectForge as well (single
   * sign-on) and has access to the area there (admin, access user or access group), not only the area's
   * password. Otherwise the backend removes it from the files.
   */
  userInfoVisible?: boolean;
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
 * the list is read from the state (there is no entity to read it from), and who uploaded a file is
 * shown only to a visitor logged in to ProjectForge with access to the area (`userInfo`) — for an external upload it is an ip
 * address. Own files (uploaded in this session) can be encrypted like their name changed.
 */
export function publicAttachmentService(
  token: string,
  {
    multiDownload,
    maxFileSize,
    maxZipSize,
    userInfo,
  }: Pick<
    AttachmentService,
    "multiDownload" | "maxFileSize" | "maxZipSize" | "userInfo"
  >,
  queryClient: QueryClient
): AttachmentService {
  const stateQueryKey = dataTransferPublicStateQueryKey(token);
  return {
    base: BASE,
    // The list is part of the state the page has just read: taken from its cache rather than asked for
    // a second time (fetchQuery calls the backend only once the state is stale or invalidated).
    fetchAttachments: async () =>
      (
        await queryClient.fetchQuery({
          queryKey: stateQueryKey,
          queryFn: ({ signal }) => fetchDataTransferPublicState(token, signal),
        })
      ).attachments ?? [],
    encryption: true,
    multiDownload,
    // The page has a "download all" of its own, limited to a gigabyte
    // (`DataTransferPublicServicesRest.isDownloadAllAvailable`).
    downloadAll: false,
    maxFileSize,
    maxZipSize,
    userInfo,
    // Mostly an expired session ("No valid login."): the state read again shows the login form then,
    // otherwise the list is brought up to date.
    onWriteError: () => {
      void queryClient.invalidateQueries({ queryKey: stateQueryKey });
      void queryClient.invalidateQueries({ queryKey: ["attachments"] });
    },
  };
}
