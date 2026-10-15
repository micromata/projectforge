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

import type { EntityWriteResult } from "./entity";

export interface SubmitMeta<Action extends string = string> {
  action: "save" | Action;
  /**
   * Called with what the write answered, for a button that has something to do *after* it went through —
   * the invoice's "save and XRechnung", which exports the state it just saved (see EInvoiceActions).
   *
   * The channel is the meta and not the return value of `handleSubmit`, because that resolves to nothing
   * whether the server wrote or refused: a refusal is a regular answer here (HTTP 406, or a toast for an
   * `AccessException`) and the submit does not throw. So a button that must not act on a refused write has
   * no other way to tell the two apart.
   */
  onWritten?: (result: EntityWriteResult) => void;
}

/** Meta of a plain save — the default, used by a form's own submit button. */
export const SAVE_META: SubmitMeta = { action: "save" };

/** Meta of the delete button, which writes without going through the form's validation. */
export const DELETE_META: SubmitMeta<"delete"> = { action: "delete" };
