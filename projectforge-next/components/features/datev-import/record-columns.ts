import type { ImportColumn } from "@/components/shared/import/import-types";

/**
 * The preview columns of the DATEV accounting record import. Year/month are the booking batch (the month
 * sheet), the date is the real voucher date — they may differ, which the hint column explains. The amount is
 * signed as stored (debit negative).
 */
export const DATEV_RECORD_IMPORT_COLUMNS: ImportColumn[] = [
  { field: "year", headerKey: "calendar.year", kind: "integer", width: 70 },
  { field: "month", headerKey: "calendar.month", kind: "month", width: 70 },
  {
    field: "satznr",
    headerKey: "fibu.buchungssatz.satznr",
    kind: "integer",
    width: 90,
  },
  { field: "datum", headerKey: "date", kind: "date", diff: true, width: 110 },
  {
    field: "dateHint",
    headerKey: "fibu.datev.import.column.hint",
    kind: "text",
    width: 220,
  },
  {
    field: "betrag",
    headerKey: "fibu.common.betrag",
    kind: "currency",
    diff: true,
    width: 130,
  },
  {
    field: "sh",
    headerKey: "finance.accountingRecord.dc",
    kind: "text",
    diff: true,
    width: 60,
  },
  {
    field: "konto",
    headerKey: "fibu.buchungssatz.konto",
    kind: "integer",
    diff: true,
    width: 90,
  },
  {
    field: "gegenKonto",
    headerKey: "fibu.buchungssatz.gegenKonto",
    kind: "integer",
    diff: true,
    width: 100,
  },
  {
    field: "kost1",
    headerKey: "fibu.kost1",
    kind: "text",
    diff: true,
    width: 110,
  },
  {
    field: "kost2",
    headerKey: "fibu.kost2",
    kind: "text",
    diff: true,
    width: 110,
  },
  {
    field: "menge",
    headerKey: "fibu.buchungssatz.menge",
    kind: "text",
    diff: true,
    width: 80,
  },
  {
    field: "beleg",
    headerKey: "fibu.buchungssatz.beleg",
    kind: "text",
    diff: true,
    width: 110,
  },
  {
    field: "text",
    headerKey: "fibu.buchungssatz.text",
    kind: "text",
    diff: true,
    width: 260,
  },
];
