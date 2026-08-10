/**
 * What a submit of an edit form is meant to do.
 *
 * Saving is the ordinary case; an entity whose backend offers further writes adds its own action
 * names (`lendOut`, `returnBook` for a book) instead of opening a second write path — they run
 * through the same submit, i.e. the same Zod validation, the same values and the same 406 handling,
 * because those endpoints save the whole entity too (BookServicesRest runs them through
 * `saveOrUpdate`). @tanstack/react-form carries this as `onSubmitMeta`, so a button calls
 * `form.handleSubmit({ action: "lendOut" })`.
 */
export interface SubmitMeta<Action extends string = string> {
  action: "save" | Action;
}

/** Meta of a plain save — the default, used by a form's own submit button. */
export const SAVE_META: SubmitMeta = { action: "save" };

/** Meta of the delete button, which writes without going through the form's validation. */
export const DELETE_META: SubmitMeta<"delete"> = { action: "delete" };
