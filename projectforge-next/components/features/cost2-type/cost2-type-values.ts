import type { Cost2TypeValues } from "./cost2-type-schema";
import type { Kost2ArtDetail } from "./types";

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`) arrives as `undefined`; every
 * value is normalised here, so no field ever holds `undefined` — which a controlled input would read as
 * "uncontrolled" and the schema as a missing value.
 */
export function toFormValues(kost2Art: Kost2ArtDetail): Cost2TypeValues {
  return {
    id: kost2Art.id ?? null,
    name: kost2Art.name ?? "",
    fakturiert: kost2Art.fakturiert ?? false,
    workFraction: kost2Art.workFraction ?? null,
    projektStandard: kost2Art.projektStandard ?? false,
    description: kost2Art.description ?? null,
  };
}

/** Blank form for a cost-2 type that doesn't exist yet — the user types its Nummer first. */
export function emptyCost2TypeValues(): Cost2TypeValues {
  return {
    id: null,
    name: "",
    fakturiert: false,
    workFraction: null,
    projektStandard: false,
    description: null,
  };
}
