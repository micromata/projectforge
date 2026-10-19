import type { LeaveAccountEntryValues } from "./leave-account-entry-schema";
import type { LeaveAccountEntryDetail } from "./types";

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`, see types.ts) arrives as `undefined`; every
 * value is normalised here, so no field ever holds `undefined`.
 */
export function toFormValues(
  entry: LeaveAccountEntryDetail
): LeaveAccountEntryValues {
  return {
    id: entry.id ?? null,
    // The reference the picker binds to: {id, displayName}, all the server needs to resolve it by id.
    employee: entry.employee
      ? {
          id: entry.employee.id,
          displayName: entry.employee.displayName ?? "",
        }
      : null,
    date: entry.date ?? null,
    amount: entry.amount ?? null,
    description: entry.description ?? null,
  };
}

/**
 * Blank form for an entry that doesn't exist yet. The backend presets today and, from the vacation account, the
 * employee shown there (`LeaveAccountEntryEntityRest.newBaseDTO`); its `/rs/leaveAccountEntry/newEntry` answer is
 * what the form is reset onto.
 */
export function emptyLeaveAccountEntryValues(): LeaveAccountEntryValues {
  return toFormValues({ id: null });
}
