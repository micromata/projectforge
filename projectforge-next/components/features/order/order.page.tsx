import { attachmentsColumn } from "@/components/shared/attachments/attachments-column";
import { TERM_KIND_IDS } from "@/lib/date-period";
import { AUFTRAG_METADATA } from "@/lib/metadata/auftrag.generated";
import { definePage } from "@/lib/page-def/define-page";
import { JiraLinkedText } from "@/components/shared/jira/jira-linked-text";
import { makeJiraFieldLinks } from "@/components/shared/jira/jira-field-links";
import { AttachmentSection } from "./edit/attachment-section";
import { OrderForecastPanel } from "./forecast/order-forecast-panel";
import { CustomerProjectFields } from "./edit/customer-project-fields";
import { OrderEditBanner } from "./edit/order-edit-banner";
import { PaymentScheduleSection } from "./edit/payment-schedule-section";
import { PositionsSection } from "./edit/positions-section";
import { SendNotificationOption } from "./edit/send-notification-option";
import {
  orderSchema,
  ORDER_FIELDS,
  ORDER_ARRAY_FIELDS,
  type OrderValues,
} from "./order-schema";
import { OrderListActions } from "./order-list-actions";
import { OrderStatisticsLine } from "./order-statistics-line";
import type { OrderStatistics } from "./order-statistics";
import { emptyOrderValues, toFormValues } from "./order-values";
import type { OrderDetail, OrderListRow } from "./types";

/** REST category of an order — `OrderEntityRest` is mapped to "order", not to "auftrag". */
export const ORDER_ENTITY = "order";
/** React Query key of the list, so a write from the edit page refreshes it. */
export const ORDER_LIST_QUERY_KEY = ["order"] as const;
/** Id of the forecast tab — what the URL carries as `?tab=forecast`. */
export const FORECAST_TAB_ID = "forecast";

// JIRA issue links below the free-text fields. The reference and the two note fields are the ones that
// carry issue keys; the title stays plain, a two-column field at the top of the head grid whose links
// row would shift the fields beside it (see makeJiraFieldLinks).
const ReferenzJiraLinks = makeJiraFieldLinks("referenz");
const StatusBeschreibungJiraLinks = makeJiraFieldLinks("statusBeschreibung");
const BemerkungJiraLinks = makeJiraFieldLinks("bemerkung");

/**
 * The whole order page — list and edit — as data (see lib/page-def/types.ts).
 *
 * This is the page the backend-driven UILayout renderer cannot express, and the reason the declaration
 * has an escape hatch at every level: two nested collections of unbounded length ([PositionsSection],
 * [PaymentScheduleSection]), sums the server computes from what is currently in the form
 * ([OrderSumsLine]), and a pair of fields that fill each other in ([CustomerProjectFields]). Everything
 * around them is ordinary — the fields, their labels, their rules and the history tab come from
 * `AuftragDO` through the generated metadata, exactly as for a book or a cost unit.
 *
 * The columns are the 19 of `OrderEntityRest.createListLayout` plus `lastUpdate` — in an order of their
 * own, and with the two ends of the period of performance as the one column they read as (`created`
 * comes on top of them, as it does for every list; see lib/page-def/audit-columns.ts). Six are computed:
 * the customer and the project are `KundeDO`/`ProjektDO` and have no `UIDataType`, so the metadata
 * cannot carry them, while the position count, the assigned persons, the person days and the four sums
 * are transient properties of `AuftragDO` (`@get:Transient`, computed by `OrderInfo`). The amounts render
 * the numeric fields rather than the `formatted*` strings the legacy list uses: a string column sorts
 * "900,00" after "1.100,00".
 */
export const ORDER_PAGE = definePage<
  OrderListRow,
  OrderValues,
  OrderDetail,
  typeof AUFTRAG_METADATA
>({
  entity: ORDER_ENTITY,
  metadata: AUFTRAG_METADATA,
  route: "/order",
  queryKey: ORDER_LIST_QUERY_KEY,
  // The order book is the reference for server-side paging: ~7000 rows, four CustomResultFilters (so
  // nothing narrows after the query) and statistics from the aggregate hook. See PageDef.serverPaging.
  serverPaging: true,
  // Where the entry sits in the main menu: Finance > Order book (MenuItemDefId.ORDER_LIST). It hangs
  // under Project management as well, for users outside the finance groups (MenuCreator).
  // `._` is the bare key of a namespace that also has children (`menu.fibu.kost`) — a title is
  // translated as it is written, unlike a column label, which falls back to it (see labelKeyFor).
  categoryKey: "menu.fibu._",
  titleKey: "fibu.auftrag.title.list",
  columns: [
    // The five columns that identify the order stay in view while the sums, the period and the rest
    // are scrolled sideways — the number is what it is referred to, the title what it is.
    { name: "nummer", size: 80, className: "font-semibold", pinned: "left" },
    { name: "erfassungsDatum", size: 110, pinned: "left" },
    {
      // The sort id is the entity's property path (`kunde`), while the row carries the DTO's name — the
      // two differ here (see Auftrag.copyTo). Sorted in memory by this very string, since it is composed
      // of number and name and no column holds it (see OrderEntityRest.filterList).
      id: "kunde.displayName",
      labelKey: "fibu.kunde._",
      accessor: (row) => row.customer?.displayName ?? "",
      size: 160,
      pinned: "left",
    },
    {
      id: "projekt.displayName",
      labelKey: "fibu.projekt._",
      accessor: (row) => row.project?.displayName ?? "",
      size: 160,
      pinned: "left",
    },
    // The row navigates to the edit page; a JIRA issue key in the title, though, links to JIRA (the
    // anchor stops the row click, see JiraLinkedText). The cell keeps the title's own emphasis, since a
    // custom cell drops the column's `className`.
    {
      name: "titel",
      size: 260,
      minSize: 180,
      className: "font-semibold text-primary",
      pinned: "left",
      cell: ({ row }) => (
        <JiraLinkedText
          text={row.original.titel}
          className="font-semibold text-primary"
        />
      ),
    },
    {
      id: "nettoSumme",
      labelKey: "fibu.auftrag.nettoSumme",
      accessor: (row) => row.nettoSumme ?? null,
      dataType: "AMOUNT",
      size: 120,
    },
    {
      id: "beauftragtNettoSumme",
      labelKey: "fibu.auftrag.commissioned",
      accessor: (row) => row.beauftragtNettoSumme ?? null,
      dataType: "AMOUNT",
      size: 120,
    },
    {
      id: "fakturiertSum",
      labelKey: "fibu.fakturiert",
      accessor: (row) => row.fakturiertSum ?? null,
      dataType: "AMOUNT",
      size: 120,
    },
    {
      id: "zuFakturierenSum",
      labelKey: "fibu.toBeInvoiced",
      accessor: (row) => row.zuFakturierenSum ?? null,
      dataType: "AMOUNT",
      size: 120,
    },
    { name: "probabilityOfOccurrence", size: 80 },
    // The one value a reader looks for first — where the order stands.
    { name: "status", size: 130 },
    // Both ends in one column, as the edit form asks for them and the filter matches them (see
    // PeriodColumn).
    {
      periodLabelKey: "fibu.periodOfPerformance._",
      begin: "periodOfPerformanceBegin",
      end: "periodOfPerformanceEnd",
      size: 190,
    },
    // "#3" — how many positions the order has. No database column, so the backend sorts it in memory,
    // by the count rather than by this string (see OrderEntityRest.filterList).
    {
      id: "pos",
      labelKey: "label.position.short",
      accessor: (row) => row.pos ?? "",
      size: 60,
      // A count, although the accessor yields a string ("#3") and no data type derives it.
      align: "right",
      className: "text-muted-foreground",
    },
    {
      id: "personDays",
      labelKey: "projectmanagement.personDays._",
      accessor: (row) => row.personDays ?? null,
      dataType: "DECIMAL",
      size: 90,
      headerLabelKey: "projectmanagement.personDays.short",
    },
    {
      name: "referenz",
      size: 120,
      cell: ({ row }) => <JiraLinkedText text={row.original.referenz} />,
    },
    attachmentsColumn<OrderListRow>(),
    // The four managers in one column, as the legacy list shows them ("PM/HOB/KAM/CP").
    {
      id: "assignedPersons",
      labelKey: "fibu.common.assignedPersons",
      accessor: (row) => row.assignedPersons ?? "",
      size: 180,
      className: "text-muted-foreground",
    },
    { name: "entscheidungsDatum", size: 110 },
    // When the order was last touched — which of two orders of the same status is the one being worked
    // on. Declared, unlike `created`, which every list offers hidden (see lib/page-def/audit-columns.ts):
    // the order book is the list where this is read, not an option of it.
    { name: "lastUpdate", size: 130 },
  ],
  // The sums over the whole result set, above the table as the legacy list shows them. The cast is where
  // the untyped `ResultSet.statistics` becomes what `OrderEntityRest.OrderStatistics` sends — see
  // PageDef.statistics for why this is the place for it.
  // Mirrors AuftragListPage's CellItemListener (first match wins):
  // 1. deleted / ABGELEHNT / ERSETZT → row-deleted (struck-through grey)
  // 2. toBeInvoiced → row-red  (highest business priority: something must be invoiced)
  // 3. BEAUFTRAGT or LOI → row-green  (active order)
  // 4. ESKALATION → row-red
  // The dates of an order are agreed terms, not calendar sections: a period of performance is asked
  // about as "the month/quarter/year from here", and "Jahr bis heute" only confuses in a book of
  // commitments that run into the future.
  filterPeriodKinds: ["termMonth", "termThreeMonths", "termYear"],
  deletedLabelKey: "order.legend.deleted",
  legend: [
    { className: "row-red", labelKey: "order.legend.toBeInvoiced" },
    { className: "row-green", labelKey: "order.legend.commissioned" },
  ],
  rowClassName: (row) => {
    if (row.status === "ABGELEHNT" || row.status === "ERSETZT")
      return "row-deleted";
    if (row.toBeInvoiced) return "row-red";
    if (row.status === "BEAUFTRAGT" || row.status === "LOI") return "row-green";
    if (row.status === "ESKALATION") return "row-red";
    return undefined;
  },
  statistics: ({ statistics, isFetching }) => (
    <OrderStatisticsLine
      statistics={statistics as OrderStatistics | undefined}
      isFetching={isFetching}
    />
  ),
  listActions: OrderListActions,
  edit: {
    schema: orderSchema,
    fieldNames: ORDER_FIELDS,
    arrayFieldNames: ORDER_ARRAY_FIELDS,
    defaultValues: emptyOrderValues,
    toFormValues,
    title: (order) => order.titel ?? "",
    newTitleKey: "fibu.auftrag.title.add",
    savedMessageKey: "message.successfullChanged",
    sections: [
      {
        id: "head",
        // The bare key of a namespace with children, hence `._` — see categoryKey above.
        titleKey: "fibu.auftrag._",
        fields: [
          // The number and the date of the offer in one cell of the three columns: neither needs a
          // third of the page, and a cell of their own would push the forecast type into the next row.
          {
            group: [
              // Assigned by the backend on the first save (`AuftragDao.getNextNumber`), and never
              // changed afterwards — but shown, because it is how an order is referred to in every
              // conversation.
              { name: "nummer", readOnly: true, maxDigits: 6 },
              { name: "angebotsDatum" },
            ],
          },
          { name: "status", emphasized: true },
          { name: "forecastType", hintKey: "fibu.auftrag.forecastType.info" },
          // Highlighted like the list's title column, so both set the same focus.
          { name: "titel", span: 2, emphasized: true },
          { name: "referenz" },
          // A full-width row of its own, so it sits below the title/reference line rather than in a
          // lonely grid cell beside it (see makeJiraFieldLinks).
          { custom: ReferenzJiraLinks, span: 3 },
          { custom: CustomerProjectFields, span: 3 },
          { name: "contactPerson" },
          { name: "projectManager" },
          { name: "headOfBusinessManager" },
          { name: "salesManager" },
          // The three dates of the order's own progress — when it was entered, when it was decided, when
          // it was assigned — as one line, which is how a reader compares them. `startsRow`, because the
          // four managers above end mid-row and the line would otherwise begin in the last column.
          { name: "erfassungsDatum", startsRow: true },
          { name: "entscheidungsDatum" },
          { name: "beauftragungsDatum" },
          {
            // One label, two dates — the way it reads on the paper the order came from. And the way it
            // is usually agreed on: a term from a start date, so the end can be picked instead of
            // counted out, and moved on as a whole when the order is renewed.
            periodLabelKey: "fibu.periodOfPerformance._",
            begin: "periodOfPerformanceBegin",
            end: "periodOfPerformanceEnd",
            periodKinds: TERM_KIND_IDS,
            paging: true,
            startsRow: true,
          },
          { name: "bindungsFrist" },
          // 0 to 100, so three digits are the most it ever shows.
          { name: "probabilityOfOccurrence", maxDigits: 3 },
        ],
      },
      {
        id: "positions",
        titleKey: "fibu.auftrag.positions",
        render: ({ id }) => <PositionsSection id={id} />,
      },
      {
        id: "paymentSchedule",
        titleKey: "fibu.auftrag.paymentschedule._",
        render: ({ id }) => <PaymentScheduleSection id={id} />,
      },
      {
        id: "notes",
        titleKey: "comment",
        fields: [
          { name: "statusBeschreibung", rows: 3, span: 3 },
          { custom: StatusBeschreibungJiraLinks, span: 3 },
          { name: "bemerkung", rows: 3, span: 3 },
          { custom: BemerkungJiraLinks, span: 3 },
        ],
      },
      {
        id: "attachments",
        // The title OrderEntityRest gives the attachment fieldset, reused rather than written again.
        titleKey: "attachment.list",
        render: ({ id }) => <AttachmentSection orderId={id} />,
      },
    ],
    editBanner: OrderEditBanner,
    // Not a field of a section: it says what the save does, so it belongs where the save is pressed.
    saveOption: SendNotificationOption,
    // The analysis is computed over the *saved* order, so it is a tab of its own rather than a section
    // of a form that may hold unsaved changes — see OrderForecastPanel.
    // `._`: the key is a text of its own *and* the parent of `fibu.auftrag.forecast.analysis.*`, which
    // the generator can only export as a nested object plus a `_` leaf.
    extraTabs: [
      {
        id: FORECAST_TAB_ID,
        labelKey: "fibu.auftrag.forecast._",
        component: OrderForecastPanel,
      },
    ],
  },
});
