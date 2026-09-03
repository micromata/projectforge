import { EINGANGSRECHNUNG_METADATA } from "@/lib/metadata/eingangsrechnung.generated";
import { definePage } from "@/lib/page-def/define-page";
import { InvoiceStatisticsLine } from "@/components/shared/invoice/invoice-statistics-line";
import type { InvoiceStatistics } from "@/components/shared/invoice/invoice-statistics";
import { AccountField } from "./edit/account-field";
import { CreditorInvoiceEditBanner } from "./edit/creditor-invoice-edit-banner";
import { PaymentFields } from "./edit/payment-fields";
import { PositionsSection } from "./edit/positions-section";
import { CreditorInvoiceListActions } from "./creditor-invoice-list-actions";
import { CreditorInvoiceTransferButton } from "./creditor-invoice-transfer-button";
import {
  creditorInvoiceSchema,
  CREDITOR_INVOICE_ARRAY_FIELDS,
  CREDITOR_INVOICE_FIELDS,
  type CreditorInvoiceValues,
} from "./creditor-invoice-schema";
import {
  emptyCreditorInvoiceValues,
  toFormValues,
} from "./creditor-invoice-values";
import type { CreditorInvoiceDetail, CreditorInvoiceListRow } from "./types";

/** REST category of an incoming invoice — `IncomingInvoiceEntityRest` is mapped to "incomingInvoice". */
export const CREDITOR_INVOICE_ENTITY = "incomingInvoice";
/** React Query key of the list, so the mass update refreshes it. */
export const CREDITOR_INVOICE_LIST_QUERY_KEY = ["incomingInvoice"] as const;
/** Route of the list; the mass update hangs below it. */
export const CREDITOR_INVOICE_ROUTE = "/creditor-invoice";

/**
 * The incoming (creditor) invoice, list and form (see lib/page-def/types.ts).
 *
 * The counterpart of the outgoing invoice (invoice.page.tsx), and a leaner one throughout: a creditor
 * invoice has no number, status or type ProjectForge assigns, no customer or project of its own, no period
 * of performance, and none of the document machinery (e-invoice, Word template, attachments, clone) the
 * outgoing invoice carries. What identifies it is its creditor and reference — free text, not references —
 * beside the DATEV account.
 *
 * The columns mirror `IncomingInvoiceEntityRest.createListLayout`: the creditor and reference the invoice is
 * known by, its subject, the account, the dates, the payment type the backend translated, the two sums and
 * the cost-unit lists. The account and the cost lists are computed — `KontoDO` has no `UIDataType` and the
 * lists are transient (`Rechnung.copyFrom4ListRow`). There is no cost-assignment-difference column and no
 * orders column: neither exists on the incoming side.
 */
export const CREDITOR_INVOICE_PAGE = definePage<
  CreditorInvoiceListRow,
  CreditorInvoiceValues,
  CreditorInvoiceDetail,
  typeof EINGANGSRECHNUNG_METADATA
>({
  entity: CREDITOR_INVOICE_ENTITY,
  metadata: EINGANGSRECHNUNG_METADATA,
  route: CREDITOR_INVOICE_ROUTE,
  queryKey: CREDITOR_INVOICE_LIST_QUERY_KEY,
  // Served one page at a time: its CustomResultFilters run inside the query and its statistics come from the
  // aggregate hook, so nothing narrows after the pipeline (see IncomingInvoiceEntityRest, PageDef.serverPaging).
  serverPaging: true,
  // Finance > Incoming invoices (MenuItemDefId.INCOMING_INVOICE_LIST). `._` is the bare key of a namespace
  // that also has children (`menu.fibu.kost`) — see labelKeyFor.
  categoryKey: "menu.fibu._",
  titleKey: "menu.fibu.eingangsrechnungen",
  columns: [
    // The two columns that name the invoice stay in view while the sums and dates scroll sideways.
    {
      name: "kreditor",
      size: 200,
      className: "font-semibold",
      pinned: "left",
    },
    { name: "referenz", size: 150, pinned: "left" },
    // What the invoice is about — the column a reader searches by when the reference is unknown.
    {
      name: "betreff",
      size: 260,
      minSize: 180,
      className: "font-semibold text-primary",
      pinned: "left",
    },
    {
      // The account of the invoice ("11400 - Kreditoren") — an entity of its own, so it has no `UIDataType`
      // and is computed from the row's `konto` reference.
      id: "konto",
      labelKey: "fibu.konto",
      accessor: (row) => row.konto?.displayName ?? "",
      size: 160,
    },
    {
      // Grouped in blocks of four for reading, as the backend formats it (`ibanFormatted`).
      id: "iban",
      labelKey: "fibu.rechnung.iban",
      accessor: (row) => row.ibanFormatted ?? row.iban ?? "",
      size: 200,
      hiddenByDefault: true,
    },
    { name: "datum", size: 110 },
    {
      // Due date, or the discount date where it comes first (`faelligkeitOrDiscountMaturity`) — labelled as
      // the due date, which is what it is in the common case.
      id: "faelligkeitOrDiscountMaturity",
      labelKey: "fibu.rechnung.faelligkeit",
      accessor: (row) => row.faelligkeitOrDiscountMaturity ?? "",
      dataType: "DATE",
      size: 110,
    },
    { name: "bezahlDatum", size: 110 },
    {
      // The payment type the backend translated, which is what the list shows and sorts by — a column over
      // the constant would sort by "BANK_TRANSFER" rather than by its word.
      id: "paymentTypeAsString",
      labelKey: "fibu.payment.type",
      accessor: (row) => row.paymentTypeAsString ?? "",
      size: 130,
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
    { name: "bemerkung", size: 200 },
    // The cost units the invoice is assigned to, as their numbers; the tooltip names them and says how much
    // went to each (`RechnungInfo.detailsAsString`, the `tooltipField` of the legacy grid).
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
  // Mirrors the legacy grid's `withGetRowClass` (first match wins): overdue and unpaid reads red, anything
  // else not yet paid blue.
  legend: [
    { className: "row-red", labelKey: "fibu.rechnung.filter.ueberfaellig" },
    { className: "row-blue", labelKey: "fibu.rechnung.offen" },
  ],
  rowClassName: (row) => {
    if (row.ueberfaellig) return "row-red";
    if (!row.bezahlDatum) return "row-blue";
    return undefined;
  },
  // The sums over the whole result set, above the table as the legacy list shows them. The cast is where
  // the untyped `ResultSet.statistics` becomes what `IncomingInvoiceEntityRest` sends — the same shape the
  // outgoing invoice uses, so the shared line renders it.
  statistics: ({ statistics, isFetching }) => (
    <InvoiceStatisticsLine
      statistics={statistics as InvoiceStatistics | undefined}
      isFetching={isFetching}
    />
  ),
  listActions: CreditorInvoiceListActions,
  // Served under `incomingInvoiceSelected` — the mass-update endpoint of this category (URL_SUFFIX_SELECTED
  // = "Selected", no dash), mirroring the outgoing invoice's `invoiceSelected`.
  massUpdate: {
    endpoint: "incomingInvoiceSelected",
    route: `${CREDITOR_INVOICE_ROUTE}/mass-update`,
    statisticsLine: ({ statistics }) => (
      <InvoiceStatisticsLine
        statistics={statistics as InvoiceStatistics | undefined}
      />
    ),
  },
  edit: {
    schema: creditorInvoiceSchema,
    fieldNames: CREDITOR_INVOICE_FIELDS,
    arrayFieldNames: CREDITOR_INVOICE_ARRAY_FIELDS,
    defaultValues: emptyCreditorInvoiceValues,
    toFormValues,
    // What the invoice is about, which is how it is referred to in a conversation.
    title: (invoice) => invoice.betreff ?? invoice.kreditor ?? "",
    newTitleKey: "fibu.rechnung.title.add",
    savedMessageKey: "message.successfullChanged",
    // The first thing a creditor invoice is written by — who it is from.
    autoFocus: "kreditor",
    // The SEPA bank transfer of this invoice, as Wicket's edit page offers it beside the heading.
    headerTrailing: (invoice) => (
      <CreditorInvoiceTransferButton invoiceId={invoice?.id} />
    ),
    sections: [
      {
        id: "head",
        titleKey: "fibu.rechnung",
        fields: [
          { name: "datum" },
          // Highlighted like the list's subject column, so both set the same focus.
          { name: "betreff", span: 2, emphasized: true },
          { name: "kreditor" },
          { name: "referenz" },
          { name: "customernr" },
          { custom: AccountField },
          { name: "receiver", span: 2 },
          { name: "iban" },
          { name: "bic" },
          { name: "paymentType" },
        ],
      },
      {
        id: "payment",
        titleKey: "fibu.rechnung.paymentTerms",
        render: ({ id }) => <PaymentFields id={id} />,
      },
      {
        id: "positions",
        titleKey: "fibu.rechnung.positions",
        render: ({ id }) => <PositionsSection id={id} />,
      },
      {
        id: "notes",
        titleKey: "comment",
        fields: [
          { name: "bemerkung", rows: 3, span: 3 },
          { name: "besonderheiten", rows: 3, span: 3 },
        ],
      },
    ],
    editBanner: CreditorInvoiceEditBanner,
  },
});
