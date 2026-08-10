import type { Cost1Values } from "./cost1-schema";
import type { Cost1Detail } from "./types";

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`, see types.ts) arrives as
 * `undefined`; every value is normalised here, so no field ever holds `undefined` — which a controlled
 * input would read as "uncontrolled" and the schema as a missing value.
 */
export function toFormValues(cost1: Cost1Detail): Cost1Values {
  return {
    id: cost1.id ?? null,
    // The DO's four parts are Kotlin `Int` and always present; `?? null` only covers a DTO that came
    // from somewhere else than Kost1PagesRest.
    nummernkreis: cost1.nummernkreis ?? null,
    bereich: cost1.bereich ?? null,
    teilbereich: cost1.teilbereich ?? null,
    endziffer: cost1.endziffer ?? null,
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
