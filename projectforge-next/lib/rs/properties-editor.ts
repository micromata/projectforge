/**
 * The properties editor on the System page (`org.projectforge.rest.admin.PropertiesEditorRest`): the
 * parameters of the admin's projectforge.properties, grouped like the bundled application.properties.
 * Values of secrets never reach the client (only whether they are set). Admin-only, self-checked on
 * the backend; saving writes a backup first, changes take effect only after a restart.
 */

import { request } from "./client";

export type PropertyType = "STRING" | "BOOLEAN";

/**
 * What ProjectForge knows about a key (`PropertiesMetadata.KeyStatus`): documented, another index of a
 * documented list key or an entry of a documented map, undocumented in ProjectForge's own namespace
 * (no longer used, or a typo), undocumented otherwise (e.g. a Spring Boot property).
 */
export type PropertyStatus = "KNOWN" | "VARIANT" | "OBSOLETE" | "UNKNOWN";

export interface PropertyParam {
  key: string;
  description?: string | null;
  defaultValue?: string | null;
  example?: string | null;
  optional: boolean;
  /** False for keys of projectforge.properties unknown to application.properties. */
  known: boolean;
  status: PropertyStatus;
  type: PropertyType;
  secret: boolean;
  inFile: boolean;
  fileValue?: string | null;
  fileComment?: string | null;
  activeValue?: string | null;
  activeSource?: string | null;
  activeDiffers: boolean;
}

export interface PropertyBlock {
  description?: string | null;
  params: PropertyParam[];
}

export interface PropertyGroup {
  id: string;
  title: string;
  expert: boolean;
  blocks: PropertyBlock[];
}

export interface PropertiesEditorData {
  filePath: string;
  fileExists: boolean;
  writable: boolean;
  fileVersion: string;
  groups: PropertyGroup[];
}

export interface PropertyChange {
  key: string;
  value?: string;
  remove: boolean;
}

export interface PropertiesSaveResponse {
  success: boolean;
  message: string;
  conflict: boolean;
}

export function fetchPropertiesEditor(
  signal?: AbortSignal
): Promise<PropertiesEditorData> {
  return request<PropertiesEditorData>(
    "/rs/propertiesEditor",
    { method: "GET" },
    signal
  );
}

export function saveProperties(
  fileVersion: string,
  changes: PropertyChange[]
): Promise<PropertiesSaveResponse> {
  return request<PropertiesSaveResponse>("/rs/propertiesEditor/save", {
    method: "POST",
    body: JSON.stringify({ fileVersion, changes }),
  });
}

/** A backup of projectforge.properties (`ProjectForgePropertiesService.BackupData`), newest first. */
export interface PropertiesBackup {
  /** The backup file's name. */
  key: string;
  /** When it was written, in epoch millis. */
  timestamp: number;
}

export function fetchPropertiesBackups(
  signal?: AbortSignal
): Promise<PropertiesBackup[]> {
  return request<PropertiesBackup[]>(
    "/rs/propertiesEditor/backups",
    { method: "GET" },
    signal
  );
}

/**
 * A version of projectforge.properties as the diff compares it (`PropertiesEditorRest.getDiffText`):
 * the file itself without [key], else the backup of that name. With [ignoreOrder], the parameters are
 * sorted, so only changed values and comments are left to see. Values of secrets are masked.
 */
export async function fetchPropertiesDiffText(
  key: string | null,
  ignoreOrder: boolean,
  signal?: AbortSignal
): Promise<string> {
  const params = new URLSearchParams({ ignoreOrder: String(ignoreOrder) });
  if (key) params.set("key", key);
  const { text } = await request<{ text: string }>(
    `/rs/propertiesEditor/diffText?${params}`,
    { method: "GET" },
    signal
  );
  return text;
}
