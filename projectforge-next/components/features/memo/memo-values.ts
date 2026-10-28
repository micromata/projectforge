import type { MemoValues } from "./memo-schema";
import type { MemoDetail } from "./types";

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`) arrives as `undefined`; every
 * value is normalised here, so no field ever holds `undefined` — which a controlled input would read as
 * "uncontrolled" and the schema as a missing value.
 */
export function toFormValues(memo: MemoDetail): MemoValues {
  return {
    id: memo.id ?? null,
    subject: memo.subject ?? null,
    memo: memo.memo ?? null,
    owner: memo.owner ?? null,
    created: memo.created ?? null,
  };
}

/**
 * Blank form for a memo that doesn't exist yet. The owner is the logged-in user, preset by the backend
 * (`MemoEntityRest.newBaseDO`), whose `/rs/memo/newEntry` answer the form is reset onto.
 */
export function emptyMemoValues(): MemoValues {
  return toFormValues({ id: null });
}
