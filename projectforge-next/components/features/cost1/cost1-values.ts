import type { Cost1Values } from "./cost1-schema";
import type { Cost1Detail } from "./types";

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`, see types.ts) arrives as
 * `undefined`; every value is normalised here, so no field ever holds `undefined` — which a controlled
 * input would read as "uncontrolled" and the schema as a missing value.
 */
export function toFormValues(cost1: Cost1Detail): Cost1Values {
  // A new entry comes from `cost1/edit` without an id, and `Kost1DO`'s four parts are Kotlin `Int`
  // with no way to say "unset" — they arrive as 0. That is the entity's default, not a proposal, so
  // the boxes stay empty rather than offering the number 0.000.00.00 (see emptyCost1Values). A saved
  // entry keeps its zeros: 0 is a valid part.
  const part = (value: number | null | undefined) =>
    cost1.id == null && value === 0 ? null : (value ?? null);
  return {
    id: cost1.id ?? null,
    nummernkreis: part(cost1.nummernkreis),
    bereich: part(cost1.bereich),
    teilbereich: part(cost1.teilbereich),
    endziffer: part(cost1.endziffer),
    kostentraegerStatus: cost1.kostentraegerStatus ?? null,
    description: cost1.description ?? null,
    created: cost1.created ?? null,
  };
}

/**
 * Blank form for a cost unit that doesn't exist yet.
 *
 * Every part starts empty rather than at 0: a 0 is a valid nummernkreis, so pre-filling one would
 * silently propose the number 0.000.00.00. The status starts unset like Wicket's (`Kost1DO`
 * `kostentraegerStatus` is nullable, and its list page treats "no status" as active).
 */
export function emptyCost1Values(): Cost1Values {
  return {
    id: null,
    nummernkreis: null,
    bereich: null,
    teilbereich: null,
    endziffer: null,
    kostentraegerStatus: null,
    description: null,
    created: null,
  };
}
