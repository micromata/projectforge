import type { ImportColumn } from "@/components/shared/import/import-types";

/** The preview columns of the DATEV chart of accounts import; only the name is imported (hence `diff`). */
export const DATEV_ACCOUNT_IMPORT_COLUMNS: ImportColumn[] = [
  {
    field: "nummer",
    headerKey: "fibu.konto.nummer",
    kind: "integer",
    width: 120,
  },
  {
    field: "bezeichnung",
    headerKey: "fibu.konto.bezeichnung",
    kind: "text",
    diff: true,
    width: 360,
  },
];
