import type { Cost2Values } from "./cost2-schema";
import type { Cost2Detail } from "./types";

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`, see types.ts) arrives as
 * `undefined`; every value is normalised here, so no field ever holds `undefined` — which a controlled
 * input would read as "uncontrolled" and the schema as a missing value.
 */
export function toFormValues(cost2: Cost2Detail): Cost2Values {
  // A new entry comes from `cost2/new` without an id, and Kost2DO's three number parts are Kotlin `Int`
  // with no way to say "unset" — they arrive as 0. That is the entity's default, not a proposal, so the
  // boxes stay empty rather than offering the number 0.000.00.00 (see emptyCost2Values). A saved entry
  // keeps its zeros: 0 is a valid part.
  const part = (value: number | null | undefined) =>
    cost2.id == null && value === 0 ? null : (value ?? null);
  return {
    id: cost2.id ?? null,
    nummernkreis: part(cost2.nummernkreis),
    bereich: part(cost2.bereich),
    teilbereich: part(cost2.teilbereich),
    endziffer: part(cost2.endziffer),
    // The reference the picker binds to: {id, displayName}, all the server needs to resolve it by id.
    project: cost2.project
      ? { id: cost2.project.id, displayName: cost2.project.name ?? "" }
      : null,
    workFraction: cost2.workFraction ?? null,
    sharedCost: cost2.sharedCost ?? null,
    kostentraegerStatus: cost2.kostentraegerStatus ?? null,
    description: cost2.description ?? null,
    comment: cost2.comment ?? null,
  };
}

/**
 * Blank form for a cost unit that doesn't exist yet.
 *
 * Every number part starts empty rather than at 0: a 0 is a valid nummernkreis, so pre-filling one
 * would silently propose the number 0.000.00.00. The status starts unset like Wicket's (Kost2DO
 * `kostentraegerStatus` is nullable, and the list treats "no status" as active).
 */
export function emptyCost2Values(): Cost2Values {
  return {
    id: null,
    nummernkreis: null,
    bereich: null,
    teilbereich: null,
    endziffer: null,
    project: null,
    workFraction: null,
    sharedCost: null,
    kostentraegerStatus: null,
    description: null,
    comment: null,
  };
}
