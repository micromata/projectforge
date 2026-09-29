/**
 * The employee-salary (Gehaltsimport) xlsx import (`EmployeeSalaryImportRest`, mapped to
 * `employeeSalaryImport`). A thin binding of the generic import client (./import.ts) to this entity's base,
 * the same way lib/rs/creditor-invoice-import.ts wraps the generic exports for its category.
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

/** REST path base of the employee-salary import — `EmployeeSalaryImportRest` is mapped here. */
export const ENTITY = "employeeSalaryImport";

export function uploadEmployeeSalaryImport(
  file: File,
  options?: UploadOptions
): Promise<UploadImportResult> {
  return uploadImportFile(ENTITY, file, options);
}

export function fetchEmployeeSalaryImportState(signal?: AbortSignal) {
  return fetchImportState(ENTITY, signal);
}

export function reconcileEmployeeSalaryImport(
  displayOptions?: DisplayOptions,
  signal?: AbortSignal
) {
  return reconcileImport(ENTITY, displayOptions, signal);
}

export function commitEmployeeSalaryImport(
  selectedIds: number[],
  displayOptions?: DisplayOptions,
  signal?: AbortSignal
): Promise<CommitImportResult> {
  return commitImport(ENTITY, selectedIds, displayOptions, signal);
}

export function cancelEmployeeSalaryImport(
  signal?: AbortSignal
): Promise<void> {
  return cancelImport(ENTITY, signal);
}
