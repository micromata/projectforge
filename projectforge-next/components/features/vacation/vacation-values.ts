import type { VacationValues } from "./vacation-schema";
import type { VacationDetail } from "./types";

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`) arrives as `undefined`; every
 * value is normalised here, so no field ever holds `undefined` — which a controlled input would read as
 * "uncontrolled" and the schema as a missing value.
 */
export function toFormValues(vacation: VacationDetail): VacationValues {
  return {
    id: vacation.id ?? null,
    employee: vacation.employee ?? null,
    // Not the first day shown of a vacation whose begin isn't told: no begin at all.
    startDate: vacation.startHidden ? null : (vacation.startDate ?? null),
    endDate: vacation.endDate ?? null,
    halfDayBegin: vacation.halfDayBegin === true,
    halfDayEnd: vacation.halfDayEnd === true,
    manager: vacation.manager ?? null,
    replacement: vacation.replacement ?? null,
    otherReplacements: vacation.otherReplacements ?? [],
    status: vacation.status ?? null,
    special: vacation.special === true,
    comment: vacation.comment ?? null,
  };
}

/**
 * Blank form for an entry that doesn't exist yet. Employee, manager, replacement and status are preset by
 * the backend (`VacationEntityRest.newBaseDTO`), whose `/rs/vacation/newEntry` answer the form is reset onto.
 */
export function emptyVacationValues(): VacationValues {
  return toFormValues({ id: null });
}
