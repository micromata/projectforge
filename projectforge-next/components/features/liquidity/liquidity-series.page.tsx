import { LIQUIDITY_SERIES_METADATA } from "@/lib/metadata/liquidity-series.generated";
import { definePage } from "@/lib/page-def/define-page";
import { SeriesEditHint } from "./edit/series-edit-hint";
import {
  liquiditySeriesSchema,
  LIQUIDITY_SERIES_FIELDS,
  type LiquiditySeriesValues,
} from "./liquidity-series-schema";
import {
  emptyLiquiditySeriesValues,
  toSeriesFormValues,
} from "./liquidity-series-values";
import { LIQUIDITY_ROUTE } from "./liquidity.page";
import type { LiquidityListRow, LiquiditySeriesDetail } from "./types";

/** REST category of the series editor — `LiquiditySeriesRest` is mapped to "liquiditySeries". */
export const LIQUIDITY_SERIES_ENTITY = "liquiditySeries";
/** Route of the focused series editor; reached from an occurrence's "part of series …" link. */
export const LIQUIDITY_SERIES_ROUTE = `${LIQUIDITY_ROUTE}/series`;

/**
 * The focused editor of a recurring [LiquiditySeriesDO] — no list of its own, only load and save. It is
 * reached from the read-only "part of series … — Edit series" link on any occurrence (see SeriesLink),
 * and changes only the rule and the template: copy-on-write means the future, still-virtual occurrences
 * follow the new template while materialized (touched/paid) ones stay frozen, so there is no
 * "this / all occurrences" prompt (see the plan).
 *
 * A full page-def only because `EntityEditPage` takes one; the list half is inert (no columns, no menu),
 * since a series is created through the entry form's "repeat" block, not added here.
 */
export const LIQUIDITY_SERIES_PAGE = definePage<
  LiquidityListRow,
  LiquiditySeriesValues,
  LiquiditySeriesDetail,
  typeof LIQUIDITY_SERIES_METADATA
>({
  entity: LIQUIDITY_SERIES_ENTITY,
  metadata: LIQUIDITY_SERIES_METADATA,
  route: LIQUIDITY_SERIES_ROUTE,
  queryKey: [LIQUIDITY_SERIES_ENTITY],
  categoryKey: "menu.reporting",
  titleKey: "plugins.liquidityplanning.series.title",
  // No list: a series has none, and none of the list machinery reads this (see the module comment).
  columns: [],
  edit: {
    schema: liquiditySeriesSchema,
    fieldNames: LIQUIDITY_SERIES_FIELDS,
    defaultValues: emptyLiquiditySeriesValues,
    toFormValues: toSeriesFormValues,
    title: (series) => series.subject ?? "",
    newTitleKey: "plugins.liquidityplanning.series.title",
    savedMessageKey: "message.successfullChanged",
    autoFocus: "startDate",
    // Cancel and a successful save return to the entry list — the series has no list to go back to.
    returnTargets: [
      {
        route: LIQUIDITY_ROUTE,
        labelKey: "plugins.liquidityplanning.entry.title.list",
      },
    ],
    sections: [
      {
        id: "series",
        titleKey: "plugins.liquidityplanning.series.title",
        fields: [
          // Up front: what a save here changes (future virtual occurrences) and what it leaves frozen.
          { custom: SeriesEditHint },
          // The recurrence rule reads as one line: from when, every how many months, for how many
          // installments (empty = endless).
          {
            span: 3,
            packed: true,
            group: [
              { name: "startDate" },
              { name: "intervalMonths", maxDigits: 3, alignNumber: "right" },
              {
                name: "count",
                maxDigits: 4,
                alignNumber: "right",
                hintKey: "plugins.liquidityplanning.series.count.endless",
              },
            ],
          },
          // The template every occurrence starts from.
          {
            span: 3,
            packed: true,
            group: [
              { name: "amount", maxDigits: 10, alignNumber: "right" },
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
    ],
  },
});
