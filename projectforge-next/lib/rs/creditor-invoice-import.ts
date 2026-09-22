/**
 * The incoming-invoice (Kreditor) CSV/DATEV import (`IncomingInvoiceImportRest`, mapped to
 * `incomingInvoiceImport`). A thin binding of the generic import client (./import.ts) to this entity's
 * base — the same way lib/rs/creditor-invoice.ts wraps the generic exports and sums for its category.
 */

import {
  cancelImport,
  commitImport,
  fetchImportState,
  reconcileImport,
  uploadImportFile,
  type CommitImportResult,
  type UploadImportResult,
} from "./import";
import { type UploadOptions } from "./upload";
import type { DisplayOptions } from "@/components/shared/import/import-types";

/** REST path base of the incoming-invoice import — `IncomingInvoiceImportRest` is mapped here. */
export const ENTITY = "incomingInvoiceImport";

export function uploadCreditorInvoiceImport(
  file: File,
  options?: UploadOptions
): Promise<UploadImportResult> {
  return uploadImportFile(ENTITY, file, options);
}

export function fetchCreditorInvoiceImportState(signal?: AbortSignal) {
  return fetchImportState(ENTITY, signal);
}

export function reconcileCreditorInvoiceImport(
  displayOptions?: DisplayOptions,
  signal?: AbortSignal
) {
  return reconcileImport(ENTITY, displayOptions, signal);
}

export function commitCreditorInvoiceImport(
  selectedIds: number[],
  displayOptions?: DisplayOptions,
  signal?: AbortSignal
): Promise<CommitImportResult> {
  return commitImport(ENTITY, selectedIds, displayOptions, signal);
}

export function cancelCreditorInvoiceImport(
  signal?: AbortSignal
): Promise<void> {
  return cancelImport(ENTITY, signal);
}
