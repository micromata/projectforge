import type { SkillEntryValues } from "./skill-entry-schema";
import type { SkillEntryDetail } from "./types";

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`) arrives as `undefined`; every
 * value is normalised here, so no field ever holds `undefined` — which a controlled input would read as
 * "uncontrolled" and the schema as a missing value. An unrated skill holds 0, as the stars show it.
 */
export function toFormValues(entry: SkillEntryDetail): SkillEntryValues {
  return {
    id: entry.id ?? null,
    skill: entry.skill ?? "",
    owner: entry.owner ?? null,
    rating: entry.rating ?? 0,
    interest: entry.interest ?? 0,
    comment: entry.comment ?? null,
    created: entry.created ?? null,
  };
}

/**
 * Blank form for a skill that doesn't exist yet. The owner is the logged-in user, preset by the backend
 * (`SkillEntryEntityRest.newBaseDO`), whose `/rs/skillentry/newEntry` answer the form is reset onto.
 */
export function emptySkillEntryValues(): SkillEntryValues {
  return toFormValues({ id: null });
}
