/**
 * What a submit of the book form is meant to do.
 *
 * All three go through the same submit — same Zod validation, same values, same 406 handling —
 * because the loan endpoints save the whole book too (BookServicesRest runs them through
 * `saveOrUpdate`). @tanstack/react-form carries this as `onSubmitMeta`, so the loan buttons call
 * `form.handleSubmit({ action: "lendOut" })` instead of opening a second write path.
 */
export interface BookSubmitMeta {
  action: "save" | "lendOut" | "returnBook";
}

/** Meta of a plain save — the default, used by the form's own submit button. */
export const SAVE_META: BookSubmitMeta = { action: "save" };
