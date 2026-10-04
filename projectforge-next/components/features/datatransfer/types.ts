// The DTO itself (org.projectforge.plugins.datatransfer.rest.DataTransferArea) is declared in
// lib/rs/datatransfer.ts, beside the calls of the file view that answer it as well.
import type { DataTransferArea } from "@/lib/rs/datatransfer";

export type DataTransferAreaDetail = DataTransferArea;

/** Projection the list page renders — the same DTO, with the id the table keys rows by. */
export interface DataTransferListRow extends DataTransferArea {
  id: number;
}
