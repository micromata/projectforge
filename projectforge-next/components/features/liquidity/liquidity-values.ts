import type { LiquidityValues } from "./liquidity-schema";
import type { LiquidityDetail } from "./types";

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`, see types.ts) arrives as
 * `undefined`; every value is normalised here, so no field ever holds `undefined` — which a controlled
 * input would read as "uncontrolled" and the schema as a missing value. The mandatory `subject` keeps ""
 * rather than null, the invariant `requiredString` relies on.
 *
 * Module level and never wrapped in a hook: `useEntityEditForm` resets the form whenever this function
 * changes identity, so a per-render one would reset on every render and throw away what is being typed.
 */
export function toFormValues(entry: LiquidityDetail): LiquidityValues {
  return {
    id: entry.id ?? null,
    dateOfPayment: entry.dateOfPayment ?? null,
    amount: entry.amount ?? null,
    // null is a value here, not a gap: it is the "automatic" state of the three-state override.
    paid: entry.paid ?? null,
    autoSetPaid: entry.autoSetPaid ?? false,
    subject: entry.subject ?? "",
    comment: entry.comment ?? null,
    created: entry.created ?? null,
  };
}

/**
 * Blank form for an entry that doesn't exist yet — the empty DO run through the very same normalisation,
 * rather than a second list of the same fields. The edit page fetches `/rs/liquidity/newEntry` for the
 * values a user actually sees; this is only the shape the form starts out with.
 */
export function emptyLiquidityValues(): LiquidityValues {
  return toFormValues({ id: null });
}
