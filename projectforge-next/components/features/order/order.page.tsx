import { attachmentsColumn } from "@/components/shared/attachments/attachments-column";
import { AUFTRAG_METADATA } from "@/lib/metadata/auftrag.generated";
import { definePage } from "@/lib/page-def/define-page";
import { AttachmentSection } from "./edit/attachment-section";
import { CustomerProjectFields } from "./edit/customer-project-fields";
import { OrderSumsLine } from "./edit/order-sums-line";
import { PaymentScheduleSection } from "./edit/payment-schedule-section";
import { PositionsSection } from "./edit/positions-section";
import { orderSchema, ORDER_FIELDS, type OrderValues } from "./order-schema";
import { emptyOrderValues, toFormValues } from "./order-values";
import type { OrderDetail, OrderListRow } from "./types";

/** REST category of an order — `AuftragPagesRest` is mapped to "order", not to "auftrag". */
export const ORDER_ENTITY = "order";
/** React Query key of the list, so a write from the edit page refreshes it. */
export const ORDER_LIST_QUERY_KEY = ["order"] as const;
/** Id of the forecast tab, so its own page can mark itself as the open one. */
export const FORECAST_TAB_ID = "forecast";

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
 * The columns are the 19 of `AuftragPagesRest.createListLayout`, in its order. Six of them are computed:
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
  // Where the entry sits in the main menu: Finance > Order book (MenuItemDefId.ORDER_LIST). It hangs
  // under Project management as well, for users outside the finance groups (MenuCreator).
  // `._` is the bare key of a namespace that also has children (`menu.fibu.kost`) — a title is
  // translated as it is written, unlike a column label, which falls back to it (see labelKeyFor).
  categoryKey: "menu.fibu._",
  titleKey: "fibu.auftrag.title.list",
  addTitleKey: "fibu.auftrag.title.add",
  searchPlaceholderKey: "order.searchPlaceholder",
  columns: [
    { name: "nummer", size: 80, className: "font-semibold" },
    {
      // Sorted on the server by the entity's property path, which is `kunde`, while the row carries the
      // DTO's name — the two differ here (see Auftrag.copyTo).
      id: "kunde.displayName",
      labelKey: "fibu.kunde._",
      accessor: (row) => row.customer?.displayName ?? "",
      size: 160,
    },
    {
      id: "projekt.displayName",
      labelKey: "fibu.projekt._",
      accessor: (row) => row.project?.displayName ?? "",
      size: 160,
    },
    // No link in the cell: the whole row navigates to the edit page.
    {
      name: "titel",
      size: 260,
      minSize: 180,
      className: "font-semibold text-primary",
    },
    // "#3" — how many positions the order has. Not sortable in the database (transient), which the
    // header offers regardless; the backend simply orders by nothing then.
    {
      id: "pos",
      labelKey: "label.position.short",
      accessor: (row) => row.pos ?? "",
      size: 60,
      // A count, although the accessor yields a string ("#3") and no data type derives it.
      align: "right",
      className: "text-muted-foreground",
    },
    attachmentsColumn<OrderListRow>(),
    {
      id: "personDays",
      labelKey: "projectmanagement.personDays._",
      accessor: (row) => row.personDays ?? null,
      dataType: "DECIMAL",
      size: 90,
      headerLabelKey: "projectmanagement.personDays.short",
    },
    { name: "referenz", size: 120 },
    // The four managers in one column, as the legacy list shows them ("PM/HOB/KAM/CP").
    {
      id: "assignedPersons",
      labelKey: "fibu.common.assignedPersons",
      accessor: (row) => row.assignedPersons ?? "",
      size: 180,
      className: "text-muted-foreground",
    },
    { name: "erfassungsDatum", size: 110 },
    { name: "entscheidungsDatum", size: 110 },
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
    { name: "periodOfPerformanceBegin", size: 110 },
    { name: "periodOfPerformanceEnd", size: 110 },
    { name: "probabilityOfOccurrence", size: 80 },
    // The one value a reader looks for first — where the order stands.
    { name: "status", size: 130 },
  ],
  edit: {
    schema: orderSchema,
    fieldNames: ORDER_FIELDS,
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
          // Assigned by the backend on the first save (`AuftragDao.getNextNumber`), and never changed
          // afterwards — but shown, because it is how an order is referred to in every conversation.
          { name: "nummer", readOnly: true, maxDigits: 6 },
          { name: "status", emphasized: true },
          { name: "forecastType", hintKey: "fibu.auftrag.forecastType.info" },
          { name: "titel", span: 2 },
          { name: "referenz" },
          { custom: CustomerProjectFields, span: 3 },
          { name: "contactPerson" },
          { name: "projectManager" },
          { name: "headOfBusinessManager" },
          { name: "salesManager" },
          { name: "erfassungsDatum" },
          { name: "angebotsDatum" },
          { name: "entscheidungsDatum" },
          { name: "bindungsFrist" },
          // 0 to 100, so three digits are the most it ever shows.
          { name: "probabilityOfOccurrence", maxDigits: 3 },
          {
            // One label, two dates — the way it reads on the paper the order came from.
            periodLabelKey: "fibu.periodOfPerformance._",
            begin: "periodOfPerformanceBegin",
            end: "periodOfPerformanceEnd",
          },
          { name: "beauftragungsDatum" },
          { name: "beauftragungsBeschreibung", span: 2 },
          { custom: OrderSumsLine, span: 3 },
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
          { name: "bemerkung", rows: 3, span: 3 },
        ],
      },
      {
        id: "attachments",
        // The title AuftragPagesRest gives the attachment fieldset, reused rather than written again.
        titleKey: "attachment.list",
        render: ({ id }) => <AttachmentSection orderId={id} />,
      },
    ],
    // The analysis is computed over the *saved* order, so it is a page of its own rather than a section
    // of a form that may hold unsaved changes — see OrderForecastPage.
    // `._`: the key is a text of its own *and* the parent of `fibu.auftrag.forecast.analysis.*`, which
    // the generator can only export as a nested object plus a `_` leaf.
    extraTabs: [{ id: FORECAST_TAB_ID, labelKey: "fibu.auftrag.forecast._" }],
  },
});
