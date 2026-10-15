import type { BUCHUNGSSATZ_METADATA } from "@/lib/metadata/buchungssatz.generated";

/** Soll/Haben (debit/credit) of an accounting record. */
export type SHType =
  (typeof BUCHUNGSSATZ_METADATA.fields.sh.enumValues)[number]["value"];

/**
 * A reference to another entity as the list/detail carries it ({@code {id, displayName}}). The index
 * signature mirrors the schema's `looseObject` (extra backend fields pass through untouched), so a detail
 * value assigns straight into the form values without a cast.
 */
export interface EntityRef {
  id: number;
  displayName?: string | null;
  [key: string]: unknown;
}

/**
 * A DATEV accounting record ("Buchungssatz"), mirroring the `Buchungssatz` REST DTO. Optional props use `?`
 * because Spring omits null fields (JsonInclude.Include.NON_NULL).
 */
export interface AccountingRecordDetail {
  id: number | null;
  /** Formatted record number `yyyy-mm-#####` (BuchungssatzDO.formattedSatzNummer); display only. */
  satznr?: string | null;
  year?: number | null;
  month?: number | null;
  /** ISO date string (LocalDate). */
  datum?: string | null;
  betrag?: number | null;
  sh?: SHType | null;
  beleg?: string | null;
  text?: string | null;
  menge?: string | null;
  comment?: string | null;
  kost1?: EntityRef | null;
  kost2?: EntityRef | null;
  konto?: EntityRef | null;
  gegenKonto?: EntityRef | null;
  created?: string | null;
  lastUpdate?: string | null;
}

export interface AccountingRecordListRow extends AccountingRecordDetail {
  id: number;
}

/** One row of the business-assessment (BWA) table, mirroring the backend `BwaRow`. */
export interface BwaRow {
  no?: string | null;
  id?: string | null;
  title?: string | null;
  amount?: number | null;
  indent: number;
  scale: number;
  unit?: string | null;
}

/** The business assessment of a result set, mirroring the backend `BwaStatistics`. */
export interface BwaStatistics {
  overallPerformance?: number | null;
  merchandisePurchase?: number | null;
  preliminaryResult?: number | null;
  rows: BwaRow[];
}

/** Response of `GET /rs/accountingRecord/reportRecords` (report drill-down). */
export interface ReportRecordsResult {
  records: AccountingRecordListRow[];
  statistics?: BwaStatistics | null;
}
