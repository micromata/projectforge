import { attachmentsColumn } from "@/components/shared/attachments/attachments-column";
import { RECHNUNG_METADATA } from "@/lib/metadata/rechnung.generated";
import { defineListPage } from "@/lib/page-def/define-page";
import { InvoiceListActions } from "./invoice-list-actions";
import { InvoiceStatisticsLine } from "./invoice-statistics-line";
import type { InvoiceStatistics } from "./invoice-statistics";
import type { InvoiceListRow } from "./types";

/** REST category of an outgoing invoice — `OutgoingInvoiceEntityRest` is mapped to "outgoingInvoice". */
export const INVOICE_ENTITY = "outgoingInvoice";
/** React Query key of the list, so the mass update refreshes it. */
export const INVOICE_LIST_QUERY_KEY = ["outgoingInvoice"] as const;
/** Route of the list; the mass update hangs below it. */
export const INVOICE_ROUTE = "/invoice";

/**
 * The invoice list as data (see lib/page-def/types.ts) — the first page declared without a form.
 *
 * `defineListPage`, not `definePage`: the invoice *form* with its positions, its cost assignments and
 * its e-invoice export is still Wicket's, so a row click and the add button lead there
 * (`NextMigration.MIGRATED["outgoingInvoice"]` is `listOnly`, and the url comes from
 * `listMeta.legacyEditPage` — see useEditTargets). Everything a list is, this page is: the columns,
 * the filters, the favorites, the column state, the two exports, the statistics and the mass update.
 *
 * The columns are the 18 of the deleted `RechnungPagesRest.createListLayout`, with the two ends of the
 * period of performance as the one column they read as (`created` and `lastUpdate` come on top of them
 * hidden, as they do for every list; see lib/page-def/audit-columns.ts). Six are computed: the
 * customer, the project and the account are entities of their own and have no `UIDataType`, while the
 * two sums and the cost unit lists are transient — computed by `RechnungInfo` and filled by
 * `Rechnung.copyFrom4ListRow`.
 */
export const INVOICE_PAGE = defineListPage<
  InvoiceListRow,
  typeof RECHNUNG_METADATA
>({
  entity: INVOICE_ENTITY,
  metadata: RECHNUNG_METADATA,
  route: INVOICE_ROUTE,
  queryKey: INVOICE_LIST_QUERY_KEY,
  // Finance > Invoices (MenuItemDefId.OUTGOING_INVOICE_LIST). `._` is the bare key of a namespace that
  // also has children (`menu.fibu.kost`) — see labelKeyFor.
  categoryKey: "menu.fibu._",
  titleKey: "fibu.rechnung.title.list",
  columns: [
    // The four columns that say which invoice this is stay in view while the sums, the dates and the
    // cost units are scrolled sideways.
    { name: "nummer", size: 120, className: "font-semibold", pinned: "left" },
    {
      // The sort id is the entity's property path (`kunde`), while the row carries the DTO's name — the
      // two differ here (see Rechnung.copyFrom4ListRow). The cell falls back to `kundeText`, the free
      // text of an invoice naming no customer of the list, which the backend already resolved into this
      // one string (`KundeFormatter`).
      id: "kunde.displayName",
      labelKey: "fibu.kunde._",
      accessor: (row) => row.customer?.displayName ?? "",
      size: 200,
      pinned: "left",
    },
    {
      id: "projekt.displayName",
      labelKey: "fibu.projekt._",
      accessor: (row) => row.project?.displayName ?? "",
      size: 180,
      pinned: "left",
    },
    // What the invoice is about — the one column a reader searches by when the number is unknown.
    {
      name: "betreff",
      size: 260,
      minSize: 180,
      className: "font-semibold text-primary",
      pinned: "left",
    },
    { name: "datum", size: 110 },
    { name: "faelligkeit", size: 110 },
    { name: "bezahlDatum", size: 110 },
    {
      // The status the backend translated, which is what the legacy list shows and sorts by — a column
      // over the constant would sort by "GEPLANT" and "GESTELLT" rather than by their words. Labelled
      // as the status, because that is what it shows — "as string" is the property's business.
      id: "statusAsString",
      labelKey: "fibu.rechnung.status",
      accessor: (row) => row.statusAsString ?? "",
      size: 100,
    },
    {
      id: "netSum",
      labelKey: "fibu.common.netto",
      accessor: (row) => row.netSum ?? null,
      dataType: "AMOUNT",
      size: 120,
    },
    {
      // The gross sum minus a discount that was taken — what the invoice actually came to.
      id: "grossSumWithDiscount",
      labelKey: "fibu.rechnung.bruttoBetrag",
      accessor: (row) => row.grossSumWithDiscount ?? null,
      dataType: "AMOUNT",
      size: 120,
    },
    attachmentsColumn<InvoiceListRow>(),
    {
      // The account of the invoice itself ("11400 - Debitoren"), not the one inherited from the project.
      id: "konto",
      labelKey: "fibu.konto",
      accessor: (row) => row.konto?.displayName ?? "",
      size: 160,
    },
    // Both ends in one column, as the invoice states them and the filter matches them (see PeriodColumn).
    {
      periodLabelKey: "fibu.periodOfPerformance._",
      begin: "periodOfPerformanceBegin",
      end: "periodOfPerformanceEnd",
      size: 190,
    },
    { name: "bemerkung", size: 200 },
    // The cost units the invoice is assigned to, as their numbers; the tooltip names them and says how
    // much went to each (`RechnungInfo.detailsAsString`, the `tooltipField` of the legacy grid).
    {
      id: "kost1List",
      labelKey: "fibu.kost1",
      accessor: (row) => row.kost1List ?? "",
      size: 150,
      tooltip: (row) => row.kost1Info,
    },
    {
      id: "kost2List",
      labelKey: "fibu.kost2",
      accessor: (row) => row.kost2List ?? "",
      size: 150,
      tooltip: (row) => row.kost2Info,
    },
  ],
  // Mirrors the legacy grid's `withGetRowClass` (first match wins): overdue and unpaid reads red,
  // anything else not yet paid blue.
  legend: [
    { className: "row-red", labelKey: "fibu.rechnung.filter.ueberfaellig" },
    { className: "row-blue", labelKey: "fibu.rechnung.offen" },
  ],
  rowClassName: (row) => {
    if (row.ueberfaellig) return "row-red";
    if (row.status !== "BEZAHLT") return "row-blue";
    return undefined;
  },
  // The sums over the whole result set, above the table as the legacy list shows them. The cast is where
  // the untyped `ResultSet.statistics` becomes what `OutgoingInvoiceEntityRest` sends — see
  // PageDef.statistics for why this is the place for it.
  statistics: ({ statistics, isFetching }) => (
    <InvoiceStatisticsLine
      statistics={statistics as InvoiceStatistics | undefined}
      isFetching={isFetching}
    />
  ),
  listActions: InvoiceListActions,
  // Served under `invoiceSelected`, not `outgoingInvoiceSelected` — see MassUpdateDef.endpoint.
  massUpdate: {
    endpoint: "invoiceSelected",
    route: `${INVOICE_ROUTE}/mass-update`,
    // The picked invoices summed up, by the same line the list shows above its table — the backend
    // answers `meta.statisticsData` in the shape this component takes. Cast here as for `statistics`
    // above: which shape it is, is this page's knowledge and no shell's.
    statisticsLine: ({ statistics }) => (
      <InvoiceStatisticsLine
        statistics={statistics as InvoiceStatistics | undefined}
      />
    ),
  },
});
