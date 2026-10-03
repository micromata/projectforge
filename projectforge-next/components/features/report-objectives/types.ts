/** Verbosity level of a BWA row, mirroring the backend `Priority`. */
export type ReportRowPriority = "LEAST" | "LOW" | "MIDDLE" | "HIGH" | "HIGHEST";

/** Mirror of `ReportObjectivesPageRest.PathEntry`. */
export interface ReportPathEntry {
  id?: string | null;
  title?: string | null;
}

/** Mirror of `ReportObjectivesPageRest.Column`: the current report (first) or one of its children. */
export interface ReportColumn {
  id?: string | null;
  title?: string | null;
  hasChildren: boolean;
}

/** Mirror of `ReportObjectivesPageRest.Row`: one BWA row with one amount per column. */
export interface ReportRow {
  no?: string | null;
  id?: string | null;
  title?: string | null;
  indent: number;
  scale: number;
  unit?: string | null;
  priority?: ReportRowPriority | null;
  amounts: (number | null)[];
}

/** Mirror of `ReportObjectivesPageRest.ReportData`. */
export interface ReportData {
  id?: string | null;
  title?: string | null;
  period?: string | null;
  path: ReportPathEntry[];
  columns: ReportColumn[];
  rows: ReportRow[];
}

/** Mirror of `ReportObjectivesPageRest.ReportObjectivesData`. */
export interface ReportObjectivesData {
  fileName?: string | null;
  /** `yyyy-MM`. */
  fromMonth: string;
  /** `yyyy-MM`. */
  toMonth: string;
  loaded: boolean;
  canShowRecords: boolean;
  report?: ReportData | null;
}
