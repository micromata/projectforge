import { z } from "zod";
import { LIQUIDITY_SERIES_METADATA } from "@/lib/metadata/liquidity-series.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import { REQUIRED } from "@/lib/validation/markers";

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

export const liquiditySeriesSchema = z.object({
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
});

export type LiquiditySeriesValues = z.infer<typeof liquiditySeriesSchema>;

/** Field names of the form, so a server validation error can be checked against what actually renders. */
export const LIQUIDITY_SERIES_FIELDS = Object.keys(
  liquiditySeriesSchema.shape
) as readonly (keyof LiquiditySeriesValues)[];
