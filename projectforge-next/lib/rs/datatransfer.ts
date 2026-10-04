/**
 * The calls of the data transfer pages besides the standard REST ones (`DataTransferAreaEntityRest`, list
 * and admin form): the file view of an area (`DataTransferFilesRest`), its activities
 * (`DataTransferAuditRest`), the personal box of a chosen user (`DataTransferPersonalBoxRest`), and the
 * choices and generated secrets of the admin form.
 *
 * The files themselves are ordinary attachments of category `datatransfer` (see ./attachments.ts).
 */

import type { Attachment } from "./attachments";
import { request } from "./client";

/** A user or group as the area references it: the id is what the backend stores. */
export type DataTransferRef = {
  id: number;
  displayName?: string;
};

/** org.projectforge.plugins.datatransfer.DataTransferAreaCapacity, as far as it is shown. */
export interface DataTransferCapacity {
  maxUploadSizeFormatted?: string | null;
  /** "1.2 MB/2 GB (0%)", translated by the backend. */
  capacityAsMessage?: string | null;
}

/**
 * org.projectforge.plugins.datatransfer.rest.DataTransferArea. Every optional property is `?`: Spring
 * leaves empty fields out of the JSON (`JsonInclude.Include.NON_NULL`).
 */
export interface DataTransferArea {
  id: number | null;
  areaName?: string | null;
  description?: string | null;
  admins?: DataTransferRef[] | null;
  adminsAsString?: string | null;
  observers?: DataTransferRef[] | null;
  observersAsString?: string | null;
  /** Whether the logged-in user observes the area (file view only). */
  userWantsToObserve?: boolean | null;
  accessGroups?: DataTransferRef[] | null;
  /** The groups' names, in the file view followed by their members. */
  accessGroupsAsString?: string | null;
  accessUsers?: DataTransferRef[] | null;
  accessUsersAsString?: string | null;
  externalDownloadEnabled?: boolean | null;
  externalUploadEnabled?: boolean | null;
  externalAccessToken?: string | null;
  /** Only for who may administer the area; nobody else ever receives it. */
  externalPassword?: string | null;
  /** The link for external users: the public page plus the access token. */
  externalLink?: string | null;
  externalAccessEnabled?: boolean | null;
  expiryDays?: number | null;
  maxUploadSizeKB?: number | null;
  /** The link to the file view of this area, for passing it on. */
  internalLink?: string | null;
  /** The personal box of a user: no admin form, no external access. */
  personalBox?: boolean | null;
  attachments?: Attachment[] | null;
  attachmentsCounter?: number | null;
  attachmentsSize?: number | null;
  attachmentsSizeFormatted?: string | null;
  capacity?: DataTransferCapacity | null;
  lastUpdateTimeAgo?: string | null;
  created?: string | null;
  lastUpdate?: string | null;
  writeAccess?: boolean;
  deleteAccess?: boolean;
}

/** `DataTransferFilesRest.View`: the area and what the logged-in user may do with it. */
export interface DataTransferView {
  area: DataTransferArea;
  /** The admin form is offered: no personal box, and the user may change the area. */
  editAccess: boolean;
  /** There are files, and not more than one gigabyte of them. */
  downloadAllAvailable: boolean;
  maxUploadSizeKB: number;
}

/** One entry of the activities, `DataTransferAuditDO` with its computed fields. */
export interface DataTransferAuditEntry {
  id: number;
  timestamp?: string | null;
  timeAgo?: string | null;
  filenameAsString?: string | null;
  description?: string | null;
  eventAsString?: string | null;
  byUserAsString?: string | null;
}

export interface DataTransferAudit {
  areaName?: string | null;
  events?: DataTransferAuditEntry[] | null;
  downloadEvents?: DataTransferAuditEntry[] | null;
}

/** `DataTransferPersonalBoxRest.PersonalBox`: `boxId` only once a user is chosen. */
export interface DataTransferPersonalBox {
  boxId?: number | null;
  user?: DataTransferRef | null;
}

/** One choice of the admin form, as the backend's `UISelectValue` sends it. */
export interface DataTransferOption {
  id: number;
  displayName: string;
}

/** `DataTransferAreaEntityRest.Options`: the choices of the admin form. */
export interface DataTransferOptions {
  expiryDays: DataTransferOption[];
  maxUploadSizes: DataTransferOption[];
  /** Gateway mode: external access is administered on another server. */
  gatewayPushEnabled: boolean;
  gatewayHost: string;
}

/** The id the backend reads as "the personal box of the logged-in user". */
export const PERSONAL_BOX_ID = -1;

export function dataTransferViewQueryKey(id: number) {
  return ["datatransfer", "view", id] as const;
}

export function fetchDataTransferView(
  id: number,
  signal?: AbortSignal
): Promise<DataTransferView> {
  return request<DataTransferView>(
    `/rs/datatransferfiles/${id}`,
    { method: "GET" },
    signal
  );
}

/** Adds the logged-in user to the observers of the area or removes them; answers the new view. */
export function observeDataTransferArea(
  id: number,
  observe: boolean
): Promise<DataTransferView> {
  return request<DataTransferView>(`/rs/datatransferfiles/observe/${id}`, {
    method: "POST",
    body: JSON.stringify({ observe }),
  });
}

/** All files of an area as one ZIP. A plain URL, like the other downloads (see attachmentDownloadUrl). */
export function dataTransferDownloadAllUrl(id: number): string {
  return `/rs/datatransferfiles/downloadAll/${id}`;
}

export function fetchDataTransferAudit(
  id: number,
  signal?: AbortSignal
): Promise<DataTransferAudit> {
  return request<DataTransferAudit>(
    `/rs/datatransferaudit/${id}`,
    { method: "GET" },
    signal
  );
}

/**
 * The personal box of the given user (created if missing), remembered as the user's last choice.
 * Without a user it answers that last choice only, with no box resolved.
 */
export function fetchPersonalBox(
  userId?: number,
  signal?: AbortSignal
): Promise<DataTransferPersonalBox> {
  const query = userId != null ? `?userId=${userId}` : "";
  return request<DataTransferPersonalBox>(
    `/rs/datatransferpersonalfiles/box${query}`,
    { method: "GET" },
    signal
  );
}

export function fetchDataTransferOptions(
  signal?: AbortSignal
): Promise<DataTransferOptions> {
  return request<DataTransferOptions>(
    "/rs/datatransfer/options",
    { method: "GET" },
    signal
  );
}

/** A new random access token; stored only when the form is saved. */
export async function renewDataTransferAccessToken(): Promise<string> {
  const result = await request<{ externalAccessToken: string }>(
    "/rs/datatransfer/renewAccessToken",
    { method: "POST" }
  );
  return result.externalAccessToken;
}

/** A new random password for the external access; stored only when the form is saved. */
export async function renewDataTransferPassword(): Promise<string> {
  const result = await request<{ externalPassword: string }>(
    "/rs/datatransfer/renewPassword",
    { method: "POST" }
  );
  return result.externalPassword;
}
