import { z } from "zod";
import { LIQUIDITY_SERIES_METADATA } from "@/lib/metadata/liquidity-series.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import { i18nMarker, REQUIRED } from "@/lib/validation/markers";

/** Today as an ISO `YYYY-MM-DD` string, comparable to the metadata date strings the form carries. */
function todayIso(): string {
  return new Date().toLocaleDateString("sv-SE");
}

/**
 * The rules of the recurring-series editor, taken from `LiquiditySeriesDO` through
 * `lib/metadata/liquidity-series.generated.ts` — the same source every field component reads.
 *
 * As on the entry form (see liquidity-schema.ts), **amount** and **subject** are marked required
 * explicitly: they are the template every occurrence starts from, and the metadata reports them as
 * optional because the DO's columns are nullable. `intervalMonths` is at least 1 (an interval of zero
 * would project every occurrence onto the start date).
 */
const m = fromMetadata(LIQUIDITY_SERIES_METADATA);

const liquiditySeriesObject = z.object({
  // null while the series is new — but there is no add page: a series is created via the entry form's
  // "repeat" block, so in practice this is always set here.
  id: z.number().nullable(),
  startDate: m.nullableString("startDate"),
  // Carried but not shown: only MONTHLY is offered, and dropping it from the form would null it on save.
  frequency: m.nullableString("frequency"),
  intervalMonths: m.intField("intervalMonths", { min: 1 }),
  // null = endless; a number caps the series at that many installments.
  count: m.intField("count", { min: 1 }),
  amount: m.decimalField("amount").refine((v): boolean => v != null, REQUIRED),
  subject: m.requiredString("subject"),
  comment: m.nullableString("comment"),
  autoSetPaid: m.booleanField("autoSetPaid"),
  created: m.nullableString("created"),
  // Transient (no metadata): the "valid from" date the editor posts. null = edit the whole series in
  // place; a date splits the series at that anchor (see SeriesEffectiveFrom and LiquiditySeriesRest.split).
  effectiveFrom: z.string().nullable(),
  // Transient (no metadata): the chosen scope of the change, so an unanswered choice ("null") is
  // distinguishable from a deliberate whole-series edit ("WHOLE"). "SPLIT" carries an effectiveFrom and
  // routes Save to the split endpoint; "WHOLE" is a plain in-place update. See SeriesEffectiveFrom.
  changeScope: z.enum(["WHOLE", "SPLIT"]).nullable(),
});

export const liquiditySeriesSchema = liquiditySeriesObject
  // Editing a stored series that has already started must say which occurrences it touches, so the
  // whole-series in-place edit (which rewrites the still-virtual past too) is never the silent default —
  // the past is only rewritten on a deliberate "WHOLE". A series entirely in the future needs no such
  // guard: there is nothing realized to protect (see SeriesEffectiveFrom, liquidity-series.page.tsx).
  .refine(
    (v) =>
      v.id == null ||
      !v.startDate ||
      v.startDate >= todayIso() ||
      v.changeScope != null,
    {
      path: ["changeScope"],
      message: i18nMarker("plugins.liquidityplanning.series.scope.required"),
    }
  );

export type LiquiditySeriesValues = z.infer<typeof liquiditySeriesObject>;

/** Field names of the form, so a server validation error can be checked against what actually renders. */
export const LIQUIDITY_SERIES_FIELDS = Object.keys(
  liquiditySeriesObject.shape
) as readonly (keyof LiquiditySeriesValues)[];
