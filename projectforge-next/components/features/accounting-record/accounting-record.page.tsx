import { BUCHUNGSSATZ_METADATA } from "@/lib/metadata/buchungssatz.generated";
import { definePage } from "@/lib/page-def/define-page";
import { AccountingRecordBwa } from "./accounting-record-bwa";
import {
  accountingRecordSchema,
  ACCOUNTING_RECORD_FIELDS,
  type AccountingRecordValues,
} from "./accounting-record-schema";
import {
  emptyAccountingRecordValues,
  toFormValues,
} from "./accounting-record-values";
import {
  GegenKontoField,
  KontoField,
  Kost1Field,
  Kost2Field,
} from "./edit-reference-fields";
import type {
  AccountingRecordDetail,
  AccountingRecordListRow,
  BwaStatistics,
} from "./types";

/** REST category of a DATEV accounting record — `AccountingRecordEntityRest` is mapped to "accountingRecord". */
export const ACCOUNTING_RECORD_ENTITY = "accountingRecord";
/** React Query key of the list, so a write from the edit page refreshes it. */
export const ACCOUNTING_RECORD_LIST_QUERY_KEY = ["accountingRecord"] as const;
/** Route of the list. */
export const ACCOUNTING_RECORD_ROUTE = "/accounting-record";

/**
 * The DATEV accounting-record page ("Buchungssätze") — list and edit — as data (see lib/page-def/types.ts).
 *
 * The columns are the ten of the Wicket `AccountingRecordListPage.createColumns`, in its order: the record
 * number (pinned), the amount, the voucher, the two cost units, the two accounts, debit/credit, and the two
 * free texts. `satznr` (the formatted number `yyyy-mm-#####`) and the four references (kost1/kost2/konto/
 * gegenKonto) are computed columns — the number is derived and does not round-trip, the references are
 * entities of their own with no `UIDataType` and carry only a `{id, displayName}`.
 *
 * Above the table sits the BWA (Betriebswirtschaftliche Auswertung), the analog of the invoice statistics
 * line: `AccountingRecordEntityRest` computes it over the whole result set (server paging) and sends it on
 * `ResultSet.statistics`; the slot casts that untyped payload to `BwaStatistics`.
 *
 * The edit form mirrors Wicket's `AccountingRecordEditForm`: every field but the `comment` is read-only, as a
 * record is DATEV-imported and only its annotation may change. `satznr` is shown as the form title (the same
 * string the list pins) rather than as a field, because it cannot round-trip.
 */
export const ACCOUNTING_RECORD_PAGE = definePage<
  AccountingRecordListRow,
  AccountingRecordValues,
  AccountingRecordDetail,
  typeof BUCHUNGSSATZ_METADATA
>({
  entity: ACCOUNTING_RECORD_ENTITY,
  metadata: BUCHUNGSSATZ_METADATA,
  route: ACCOUNTING_RECORD_ROUTE,
  queryKey: ACCOUNTING_RECORD_LIST_QUERY_KEY,
  // Served one page at a time: the booking-period filter runs inside the query and the BWA comes from the
  // aggregate hook, so the statistics reflect the whole filtered result set, not the visible page.
  serverPaging: true,
  // Finance > Accounting records (MenuItemDefId.ACCOUNTING_RECORD_LIST). `._` is the bare key of a
  // namespace that also has children — see labelKeyFor.
  categoryKey: "menu.fibu._",
  titleKey: "fibu.buchungssatz.title.list",
  columns: [
    // The formatted record number ("2024-01-00042") reads and sorts as one; no column of the entity holds
    // it (it is built from year/month/satznr), so it is computed and pinned like every list's identity.
    {
      id: "satznr",
      labelKey: "fibu.buchungssatz.satznr",
      accessor: (row) => row.satznr ?? "",
      size: 130,
      className: "font-mono font-semibold",
      pinned: "left",
    },
    { name: "betrag", size: 120 },
    { name: "beleg", size: 110 },
    // The cost units and accounts are entities of their own with no `UIDataType`; the row carries only
    // their `{id, displayName}` (Buchungssatz.copyFrom), so they are shown as their display name.
    {
      id: "kost1.displayName",
      labelKey: "fibu.kost1",
      accessor: (row) => row.kost1?.displayName ?? "",
      referenceKey: "kost1",
      size: 150,
    },
    {
      id: "kost2.displayName",
      labelKey: "fibu.kost2",
      accessor: (row) => row.kost2?.displayName ?? "",
      referenceKey: "kost2",
      size: 150,
    },
    {
      id: "konto.displayName",
      labelKey: "fibu.buchungssatz.konto",
      accessor: (row) => row.konto?.displayName ?? "",
      referenceKey: "konto",
      size: 160,
    },
    {
      id: "gegenKonto.displayName",
      labelKey: "fibu.buchungssatz.gegenKonto",
      accessor: (row) => row.gegenKonto?.displayName ?? "",
      referenceKey: "gegenKonto",
      size: 160,
    },
    // Debit/credit ("S"/"H"), which the enum renderer shows as its translated label.
    { name: "sh", size: 80 },
    { name: "text", size: 260 },
    { name: "comment", size: 220 },
  ],
  // The BWA over the whole filtered result set, above the table — the analog of the invoice statistics
  // line. The cast is where the untyped `ResultSet.statistics` becomes what `AccountingRecordEntityRest`
  // sends (see PageDef.statistics).
  statistics: ({ statistics, isFetching }) => (
    <AccountingRecordBwa
      statistics={statistics as BwaStatistics | undefined}
      isFetching={isFetching}
    />
  ),
  edit: {
    schema: accountingRecordSchema,
    fieldNames: ACCOUNTING_RECORD_FIELDS,
    defaultValues: emptyAccountingRecordValues,
    toFormValues,
    // The formatted record number, which is how a booking is referred to — the same string the list pins.
    title: (record) => record.satznr ?? "",
    newTitleKey: "fibu.buchungssatz.title.add",
    savedMessageKey: "message.successfullChanged",
    // The one field a user may change on an imported record.
    autoFocus: "comment",
    sections: [
      {
        id: "general",
        titleKey: "fibu.buchungssatz._",
        fields: [
          // The booking date and its fiscal year/month, read-only as in Wicket.
          { name: "datum", readOnly: true },
          {
            group: [
              { name: "year", readOnly: true },
              { name: "month", readOnly: true },
            ],
          },
          // Amount and debit/credit, the way the record states them.
          {
            group: [
              { name: "betrag", readOnly: true },
              { name: "sh", readOnly: true },
            ],
          },
          { name: "beleg", readOnly: true },
          { custom: Kost1Field },
          { custom: Kost2Field },
          { custom: KontoField },
          { custom: GegenKontoField },
          { name: "text", span: 2, readOnly: true },
          { name: "menge", readOnly: true },
          // The only editable field — the annotation a user may add to an imported booking.
          { name: "comment", span: 3, rows: 3 },
        ],
      },
    ],
  },
});
