import { LIQUIDITY_ENTRY_METADATA } from "@/lib/metadata/liquidity-entry.generated";
import { definePage } from "@/lib/page-def/define-page";
import { LiquidityListActions } from "./liquidity-list-actions";
import { LiquidityStatisticsLine } from "./liquidity-statistics-line";
import type { LiquidityStatistics } from "./liquidity-statistics";
import {
  liquiditySchema,
  LIQUIDITY_FIELDS,
  type LiquidityValues,
} from "./liquidity-schema";
import { emptyLiquidityValues, toFormValues } from "./liquidity-values";
import { PaidSelectField } from "./edit/paid-fields";
import { RepeatFields } from "./edit/repeat-fields";
import { SeriesLink } from "./edit/series-link";
import { LiquiditySubjectCell } from "./liquidity-subject-cell";
import type { LiquidityDetail, LiquidityListRow } from "./types";
import { isVirtualRow } from "./types";

/** REST category of the liquidity plugin — `LiquidityEntityRest` is mapped to "liquidity". */
export const LIQUIDITY_ENTITY = "liquidity";
/** React Query key of the list. */
export const LIQUIDITY_LIST_QUERY_KEY = ["liquidity"] as const;
/** Route of the list; the edit page hangs below it as `/liquidity/{id}`. */
export const LIQUIDITY_ROUTE = "/liquidity";

/** Today as ISO `yyyy-MM-dd` in the browser's local zone — the reference the row highlight compares against. */
function isoToday(): string {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return `${now.getFullYear()}-${month}-${day}`;
}

/**
 * The liquidity plugin's entry list and form (see lib/page-def/types.ts), the successor of the Wicket
 * `LiquidityEntryListPage` / `LiquidityEntryEditForm`.
 *
 * The entity is a plain five-field DO transferred as-is (no DTO), so the row and the detail are the same
 * shape. Not server-paged: `LiquidityEntityRest` narrows the result with `CustomResultFilter`s and computes
 * its statistics over the whole result, both of which need the full result set in one call.
 *
 * The forecast ("Liquiditätsvorschau") is not a slot of this declaration — it is a tab of the `/liquidity`
 * route beside the list (see app/(authenticated)/liquidity/page.tsx and LiquidityForecastView).
 */
export const LIQUIDITY_PAGE = definePage<
  LiquidityListRow,
  LiquidityValues,
  LiquidityDetail,
  typeof LIQUIDITY_ENTRY_METADATA
>({
  entity: LIQUIDITY_ENTITY,
  metadata: LIQUIDITY_ENTRY_METADATA,
  route: LIQUIDITY_ROUTE,
  queryKey: LIQUIDITY_LIST_QUERY_KEY,
  // Reporting > Liquidity planning (MenuItemDefId.REPORTING).
  categoryKey: "menu.reporting",
  titleKey: "plugins.liquidityplanning.entry.title.list",
  columns: [
    { name: "dateOfPayment", size: 130, pinned: "left" },
    {
      // The amount is a `DECIMAL` field, but it is money and reads as money (right-aligned, in the user's
      // currency) — a computed column over the field so it can carry the `AMOUNT` data type the field's
      // metadata doesn't.
      id: "amount",
      labelKey: "fibu.common.betrag",
      accessor: (row) => row.amount ?? null,
      dataType: "AMOUNT",
      size: 140,
    },
    {
      // The paid status the entry is actually judged by: the manual override, or — while it is
      // "automatic" — the autoSetPaid rule the backend applied (`effectivePaid`). A computed column so
      // it reads the derived value rather than the raw `paid`, which may be null (= automatic).
      id: "paid",
      labelKey: "fibu.rechnung.status.bezahlt",
      accessor: (row) => row.effectivePaid ?? false,
      dataType: "BOOLEAN",
      size: 90,
    },
    {
      name: "subject",
      size: 320,
      minSize: 200,
      className: "font-semibold text-primary",
      pinned: "left",
      // Marks the series entries with a "Series" badge — the projected virtual occurrences and the
      // materialized ones alike (see LiquiditySubjectCell).
      cell: (ctx) => <LiquiditySubjectCell row={ctx.row.original} />,
    },
    { name: "comment", size: 280 },
    // Whether the paid status is derived after the date of payment; off by default, an occasional detail.
    { name: "autoSetPaid", size: 110, hiddenByDefault: true },
  ],
  // Mirrors the Wicket list's row colours (`LiquidityEntryListPage`): an unpaid entry whose date of payment
  // is in the past (or missing) reads red (overdue), an unpaid future one blue.
  legend: [
    { className: "row-red", labelKey: "fibu.rechnung.filter.ueberfaellig" },
    { className: "row-blue", labelKey: "fibu.rechnung.offen" },
  ],
  rowClassName: (row) => {
    if (row.effectivePaid) return undefined;
    if (!row.dateOfPayment || row.dateOfPayment < isoToday()) return "row-red";
    return "row-blue";
  },
  // A virtual (projected) occurrence has no stored row to open: clicking it opens the add form prefilled
  // from the series (`newEntryParams` forwards the two to `newEntry`), and the first save materializes it.
  // Every other row falls through to the normal edit page (returning undefined).
  onRowClick: (row) => {
    if (!isVirtualRow(row) || row.seriesId == null || !row.seriesDate) {
      return undefined;
    }
    const params = new URLSearchParams({
      seriesId: String(row.seriesId),
      seriesDate: row.seriesDate,
    });
    return `${LIQUIDITY_ROUTE}/new?${params.toString()}`;
  },
  // The sums over the whole result set, above the table as the Wicket list shows them. The cast is where the
  // untyped `ResultSet.statistics` becomes what `LiquidityEntityRest` sends.
  statistics: ({ statistics, isFetching }) => (
    <LiquidityStatisticsLine
      statistics={statistics as LiquidityStatistics | undefined}
      isFetching={isFetching}
    />
  ),
  listActions: LiquidityListActions,
  // Served under `liquiditySelected` — the mass-update endpoint of this category (URL_SUFFIX_SELECTED =
  // "Selected", no dash), the counterpart of the invoice's `invoiceSelected` (see
  // LiquidityMultiSelectedPageRest). Its presence turns on the selection UI in the generic list.
  massUpdate: {
    endpoint: "liquiditySelected",
    route: `${LIQUIDITY_ROUTE}/mass-update`,
    statisticsLine: ({ statistics }) => (
      <LiquidityStatisticsLine
        statistics={statistics as LiquidityStatistics | undefined}
      />
    ),
  },
  edit: {
    schema: liquiditySchema,
    fieldNames: LIQUIDITY_FIELDS,
    defaultValues: emptyLiquidityValues,
    toFormValues,
    // What the entry is about, which is how it is referred to in a conversation.
    title: (entry) => entry.subject ?? "",
    newTitleKey: "plugins.liquidityplanning.entry.title.add",
    savedMessageKey: "message.successfullChanged",
    autoFocus: "dateOfPayment",
    // Built from the entry on screen and opened unsaved for the user to save (CloneSupport.CLONE).
    clone: true,
    // Forwarded to `newEntry` so the add form can be prefilled from a series occurrence — the seam a
    // click on a virtual row uses to materialize it (see onRowClick and LiquidityEntityRest.newBaseDO).
    newEntryParams: ["seriesId", "seriesDate"],
    sections: [
      {
        id: "entry",
        titleKey: "plugins.liquidityplanning.entry.title.heading",
        fields: [
          // Date, amount, the three-state paid override and the autoSetPaid rule read as one line — when
          // is it due, how much, is it paid, and should that be decided automatically. `maxDigits` keeps
          // the amount box just wide enough for the value, so the four share the row.
          {
            span: 3,
            packed: true,
            group: [
              { name: "dateOfPayment" },
              { name: "amount", maxDigits: 10, alignNumber: "right" },
              { custom: PaidSelectField },
              {
                name: "autoSetPaid",
                hintKey: "plugins.liquidityplanning.entry.autoSetPaid.info",
              },
            ],
          },
          { name: "subject", span: 2 },
          { name: "comment", rows: 4, span: 3 },
        ],
      },
      // Turning a new entry into a recurring series — shown only for a new, series-free entry (a series
      // can't spawn another). `data` is the loaded/preset entity: id null and no seriesId means "new and
      // not an occurrence" (see RepeatFields).
      {
        id: "repeat",
        titleKey: "plugins.liquidityplanning.series.repeat",
        collapsed: true,
        visible: ({ data }) => {
          const entry = data as unknown as LiquidityDetail | undefined;
          return entry != null && entry.id == null && entry.seriesId == null;
        },
        fields: [{ custom: RepeatFields }],
      },
      // The read-only pointer back to the series, shown instead of the repeat block for any occurrence
      // (materialized, or being materialized with seriesId preset). See SeriesLink.
      {
        id: "series",
        titleKey: "plugins.liquidityplanning.series.title",
        visible: ({ data }) =>
          (data as unknown as LiquidityDetail | undefined)?.seriesId != null,
        fields: [{ custom: SeriesLink }],
      },
    ],
  },
});
