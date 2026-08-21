import { attachmentsColumn } from "@/components/shared/attachments/attachments-column";
import { RECHNUNG_METADATA } from "@/lib/metadata/rechnung.generated";
import { definePage } from "@/lib/page-def/define-page";
import { CostAssignmentCell } from "./cost-assignment-cell";
import { AccountField } from "./edit/account-field";
import { AttachmentSection } from "./edit/attachment-section";
import { CustomerProjectFields } from "./edit/customer-project-fields";
import { EInvoiceSection } from "./edit/e-invoice-section";
import { InvoiceEditBanner } from "./edit/invoice-edit-banner";
import { InvoiceExportMenu } from "./edit/invoice-export-menu";
import { PaymentTermsFields } from "./edit/payment-terms-fields";
import { PositionsSection } from "./edit/positions-section";
import { SellerBankAccountField } from "./edit/seller-bank-account-field";
import { InvoiceListActions } from "./invoice-list-actions";
import {
  invoiceSchema,
  INVOICE_ARRAY_FIELDS,
  INVOICE_FIELDS,
  type InvoiceValues,
} from "./invoice-schema";
import { InvoiceStatisticsLine } from "./invoice-statistics-line";
import type { InvoiceStatistics } from "./invoice-statistics";
import { emptyInvoiceValues, toFormValues } from "./invoice-values";
import type { InvoiceDetail, InvoiceListRow } from "./types";

/** REST category of an outgoing invoice — `OutgoingInvoiceEntityRest` is mapped to "outgoingInvoice". */
export const INVOICE_ENTITY = "outgoingInvoice";
/** React Query key of the list, so the mass update refreshes it. */
export const INVOICE_LIST_QUERY_KEY = ["outgoingInvoice"] as const;
/** Route of the list; the mass update hangs below it. */
export const INVOICE_ROUTE = "/invoice";

/**
 * The invoice, list and form (see lib/page-def/types.ts).
 *
 * Released: `NextMigration.MIGRATED["outgoingInvoice"]` names `invoice/:id` and `invoice/new` and is no
 * longer `listOnly`, so a row click, the add button and every server side redirect after a save stay in
 * this app. Wicket's form remains reachable through the escape hatch of the page (LegacyPageLink), which
 * is the only way there now.
 *
 * Every document function Wicket has is here as well: the Word export beside the heading (InvoiceExportMenu
 * on `headerTrailing`), and the whole e-invoice as a part of the form rather than a dialog — what Wicket
 * collects in its `EInvoiceModalDialog` (the address block, the bank account, the invoice PDF, the checklist
 * and the two exports) is the `customer` section here, fields plus footer (see EInvoiceSection). It sits at
 * the end of the form, right above the attachments its ZUGFeRD export embeds.
 *
 * The columns are the 18 of the deleted `RechnungPagesRest.createListLayout`, with the two ends of the
 * period of performance as the one column they read as (`created` and `lastUpdate` come on top of them
 * hidden, as they do for every list; see lib/page-def/audit-columns.ts). Six are computed: the
 * customer, the project and the account are entities of their own and have no `UIDataType`, while the
 * two sums and the cost unit lists are transient — computed by `RechnungInfo` and filled by
 * `Rechnung.copyFrom4ListRow`.
 *
 * One column is new: the cost assignment difference, which replaces Wicket's `showKostZuweisungStatus`
 * checkbox. That switch appended the amount to the first cell of every row it didn't add up for; here it
 * is a column that can be sorted and switched off, and the question itself ("show only those") is a
 * filter of the backend (`OutgoingInvoiceEntityRest.COST_ASSIGNMENT_FILTER`).
 */
export const INVOICE_PAGE = definePage<
  InvoiceListRow,
  InvoiceValues,
  InvoiceDetail,
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
    // What Wicket's "Kostzuweisungsstatus" switch marked the rows with, as a column: the part of the net
    // sum no cost unit was assigned to yet. Off at first — it is read by whoever books the costs and is
    // noise to everyone else — and empty for an installation without cost accounting, which sends none
    // (see `Rechnung.copyFrom4ListRow`).
    {
      id: "kostZuweisungenFehlbetrag",
      labelKey: "fibu.rechnung.kostZuweisungFehlbetrag",
      accessor: (row) => row.kostZuweisungenFehlbetrag ?? null,
      dataType: "AMOUNT",
      size: 150,
      hiddenByDefault: true,
      // Its own cell, because the default amount renderer paints every sum alike and this one has to say
      // that it isn't zero — the red the form's line and the position rows use for the same number.
      cell: ({ getValue }) => (
        <CostAssignmentCell value={getValue() as number | null} />
      ),
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
  edit: {
    schema: invoiceSchema,
    fieldNames: INVOICE_FIELDS,
    arrayFieldNames: INVOICE_ARRAY_FIELDS,
    defaultValues: emptyInvoiceValues,
    toFormValues,
    // What the invoice is about, which is how it is referred to in a conversation; the number is in the
    // banner, where it stays in view.
    title: (invoice) => invoice.betreff ?? "",
    newTitleKey: "fibu.rechnung.title.add",
    savedMessageKey: "message.successfullChanged",
    // The recurring monthly invoice: the next one is the last one with a new date, so it is written by
    // cloning it (see OutgoingInvoiceEntityRest.prepareClone for what a clone keeps and what it drops).
    clone: true,
    // The one write the invoice offers besides the save: the first half of the e-invoice section's two
    // buttons, which saves and then *stays* on the page, unlike the save — the export that follows it is
    // built from what it wrote (see lib/rs/submit-meta.ts, EInvoiceActions).
    actions: ["saveAndCheckEInvoice"],
    // The Word export, beside the heading: it acts on the stored invoice, and `headerTrailing` is the one
    // slot of an edit page that is handed exactly that (see InvoiceExportMenu).
    headerTrailing: (invoice) => <InvoiceExportMenu invoiceId={invoice?.id} />,
    sections: [
      {
        id: "head",
        titleKey: "fibu.rechnung",
        fields: [
          // Number and date in one cell of the three columns: neither needs a third of the page, and the
          // two together are what identifies the invoice on paper.
          {
            group: [
              // Assigned by `RechnungDao.onInsertOrModify` on the transition out of GEPLANT, and absent
              // from a credit note the customer announced — read-only, but shown, because it is what an
              // invoice is called.
              { name: "nummer", readOnly: true, maxDigits: 8 },
              { name: "datum" },
            ],
          },
          { name: "status", emphasized: true },
          { name: "typ" },
          { name: "betreff", span: 2 },
          { custom: AccountField },
          { custom: CustomerProjectFields, span: 3 },
          // Two free texts of the invoice head, both `TextArea` in Wicket and both about what the
          // customer needs to see on it.
          { name: "customerref1", rows: 2, span: 2 },
          { name: "attachment", rows: 2 },
          {
            // One label, two dates — the way the invoice states it. The positions may each have one of
            // their own; this is the default they inherit (`PeriodOfPerformanceType.SEEABOVE`).
            periodLabelKey: "fibu.periodOfPerformance._",
            begin: "periodOfPerformanceBegin",
            end: "periodOfPerformanceEnd",
            startsRow: true,
          },
        ],
      },
      {
        id: "payment",
        titleKey: "fibu.rechnung.paymentTerms",
        render: ({ id }) => <PaymentTermsFields id={id} />,
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
      {
        // Last but one, directly above the attachments: it is the end of the invoice, and its ZUGFeRD
        // export is what embeds them — so the two sections read in the order they depend on each other.
        id: "customer",
        // The address of the recipient as the e-invoice needs it — Wicket's `fibu.konto.eInvoice`
        // fieldset, whose fields are named after the account's they are prefilled from.
        titleKey: "fibu.konto.eInvoice",
        fields: [
          { name: "customerContactPerson" },
          { name: "customerAddress", rows: 2 },
          { name: "customerZipCode", startsRow: true },
          { name: "customerCity" },
          { name: "customerCountry" },
          { name: "customerVatId", startsRow: true },
          {
            name: "customerLeitwegId",
            hintKey: "fibu.konto.leitwegId.tooltip",
          },
          { name: "customerEInvoiceEmail" },
          // A select over the configured bank accounts, as Wicket has it — custom because the options
          // are application configuration, which no field metadata can carry.
          { custom: SellerBankAccountField, startsRow: true },
        ],
        // Below those very fields: the invoice PDF, what is still missing for an e-invoice, and the three
        // buttons — the fields above are what the checklist is about (see EInvoiceSection).
        footer: EInvoiceSection,
      },
      {
        id: "attachments",
        // The title OutgoingInvoiceEntityRest gives the attachment fieldset, reused rather than written
        // again.
        titleKey: "attachment.list",
        render: ({ id }) => <AttachmentSection invoiceId={id} />,
      },
    ],
    editBanner: InvoiceEditBanner,
  },
});
