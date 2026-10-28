/**
 * The VCF import of addresses (`AddressImportRest`, `/rs/addressImport`).
 *
 * Unlike the generic import (./import.ts) there is no commit: after the upload the backend has matched every
 * vCard against the database, and the user takes the entries over one by one in the address form, which asks
 * for the comparison of its entry ([fetchAddressImportEntry]) and marks it as imported on save.
 *
 * The view and its entries have the shape of `ImportView`/`ImportEntry`, so the shared import cells and the
 * statistics line render them as they are.
 */

import { rawRequest, request, RsError } from "./client";
import { uploadWithProgress, type UploadOptions } from "./upload";
import type {
  ImportEntry,
  ImportRead,
  ImportStorageInfo,
} from "@/components/shared/import/import-types";

/** One entry of the import (`AddressImportEntryData`); its `id` is the index in the import. */
export interface AddressImportEntry extends ImportEntry<ImportRead> {
  /** The matching address, absent for a new one. */
  addressId?: number | null;
  /** The changed fields that have no column of their own, translated. */
  additionalChanges?: string | null;
  hasImage?: boolean;
}

/** The import in the session (`AddressImportView`); without a `filename` there is none. */
export interface AddressImportView {
  filename?: string | null;
  hasBeenReconciled: boolean;
  info?: ImportStorageInfo;
  entries: AddressImportEntry[];
}

/** The vCard of one entry next to the stored address (`AddressImportEntryComparison`). */
export interface AddressImportEntryComparison {
  importIndex: number;
  addressId?: number | null;
  status: string;
  /** Field name → the value of the vCard and the stored one. */
  vcfComparisonData: Record<
    string,
    { vcf?: string | null; db?: string | null }
  >;
  hasImage: boolean;
  imageTooLarge: boolean;
}

export type AddressImportUploadResult =
  | { kind: "ok"; view: AddressImportView }
  | { kind: "rejected"; error: string };

const BASE = "/rs/addressImport";

export const ADDRESS_IMPORT_QUERY_KEY = ["addressImport"] as const;

export function addressImportEntryQueryKey(index: number) {
  return [...ADDRESS_IMPORT_QUERY_KEY, "entry", index] as const;
}

/** Url of the image of an entry's vCard. */
export function addressImportImageUrl(index: number): string {
  return `${BASE}/imagePreview/${index}`;
}

export function fetchAddressImport(
  signal?: AbortSignal
): Promise<AddressImportView> {
  return request<AddressImportView>(`${BASE}/state`, { method: "GET" }, signal);
}

/** Uploads a VCF file; a refusal is a 400 carrying the (translated) reason, returned rather than thrown. */
export async function uploadAddressImport(
  file: File,
  options: UploadOptions = {}
): Promise<AddressImportUploadResult> {
  const body = new FormData();
  body.append("file", file);
  const res = await uploadWithProgress(`${BASE}/upload`, body, options);
  const parsed = parseJson(res.text);
  if (res.status >= 200 && res.status < 300) {
    return { kind: "ok", view: parsed as AddressImportView };
  }
  return {
    kind: "rejected",
    error: (parsed as { error?: string } | null)?.error ?? `${res.status}`,
  };
}

export function fetchAddressImportEntry(
  index: number,
  signal?: AbortSignal
): Promise<AddressImportEntryComparison> {
  return request<AddressImportEntryComparison>(
    `${BASE}/entry/${index}`,
    { method: "GET" },
    signal
  );
}

/** Drops the import from the session. */
export async function cancelAddressImport(): Promise<void> {
  const path = `${BASE}/cancel`;
  const res = await rawRequest(path, { method: "POST" });
  if (!res.ok) throw new RsError(res.status, `${res.status}: ${path}`);
}

function parseJson(text: string): unknown {
  try {
    return text ? JSON.parse(text) : null;
  } catch {
    return null;
  }
}
