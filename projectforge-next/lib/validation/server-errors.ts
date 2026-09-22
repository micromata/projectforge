/**
 * Puts the server's validation errors onto the fields of a `@tanstack/react-form` form.
 *
 * The server is the authority on validation (see MIGRATION.md): a hand built form's Zod schema only
 * anticipates the rules for faster feedback, while `AbstractPagesRest` answers HTTP 406 with
 * `validationErrors` whenever it disagrees — including rules the client cannot know at all, such as
 * "signature already exists". Without this the stricter server rule stays invisible: the save fails
 * and the form looks fine.
 *
 * Errors land in the `onServer` slot, which the form clears by itself as soon as the field changes
 * or blurs (see form-core's ValidationLogic) — the user retyping the value makes the stale server
 * complaint disappear without any bookkeeping here.
 */

import type { ValidationError } from "@/lib/rs/types";

/** The `onServer` slot of a form's error map: one message per field, plus a form wide one. */
interface ServerErrorMap {
  onServer?: { form?: string; fields: Record<string, string> };
}

/**
 * The bit of `FormApi` used here, so this stays free of the form's generic parameters.
 *
 * `never` as the parameter type is what makes any `FormApi` assignable: form-core derives the type
 * of the `onServer` slot from an `onServer` *validator*, and a form that has none (ours: the server
 * validates, it doesn't hand us a validator) gets `undefined` there — even though setErrorMap reads
 * the slot at runtime regardless. Hence the cast in [setServerErrors].
 */
interface ErrorMapTarget {
  setErrorMap: (errorMap: never) => void;
}

function setServerErrors(form: ErrorMapTarget, errorMap: ServerErrorMap): void {
  form.setErrorMap(errorMap as never);
}

export interface ServerValidationResult {
  /**
   * Messages the form cannot show next to a field, either because the error names no field or
   * because the form has no such field. The caller should surface them, e.g. as a toast — otherwise
   * the save silently does nothing.
   */
  unassigned: string[];
  /** True when at least one error was written into a known field's error slot. */
  hasAssigned: boolean;
}

/**
 * True when the form has a mounted field that will display this error.
 *
 * - A plain field name (`"titel"`) is known when it is in `knownFields` and not in `arrayFields`.
 * - An indexed path (`"positionen[0].titel"`) is known when the collection root is in `knownFields`:
 *   the row's `<form.Field>` renders it, and checking the root is enough because errors for all rows
 *   arrive at once while the fields only exist once the row is rendered.
 * - A bare collection name (`"positionen"`) is NOT known: no `<form.Field name="positionen">` is
 *   mounted, so TanStack Form would silently drop the error. It surfaces as a toast instead.
 */
function isKnown(
  fieldId: string,
  knownFields: readonly string[],
  arrayFields: readonly string[]
): boolean {
  // Indexed path into a collection: positionen[0].titel
  const root = fieldId.match(/^([^[.]+)\[\d+]\./)?.[1];
  if (root) return knownFields.includes(root);
  // Plain field, but not a bare array name (no form.Field is mounted for the collection itself).
  return knownFields.includes(fieldId) && !arrayFields.includes(fieldId);
}

/**
 * @param knownFields Field names the form actually renders. An error for anything else would be
 * written into a field that never displays it, so it is reported back as unassigned instead.
 * @param arrayFields Names of array (collection) fields. A bare error on one of these has no mounted
 * `<form.Field>` and must travel through `unassigned` rather than being silently dropped.
 */
export function applyServerValidationErrors(
  form: ErrorMapTarget,
  errors: ValidationError[],
  knownFields: readonly string[],
  arrayFields: readonly string[] = []
): ServerValidationResult {
  const fields: Record<string, string> = {};
  const unassigned: string[] = [];

  for (const error of errors) {
    // The backend always translates the message before sending it (ValidationError.create).
    const message = error.message?.trim();
    if (!message) continue;
    if (error.fieldId && isKnown(error.fieldId, knownFields, arrayFields)) {
      // Several errors on one field: keep them all rather than let the last one win.
      fields[error.fieldId] = fields[error.fieldId]
        ? `${fields[error.fieldId]}. ${message}`
        : message;
    } else {
      unassigned.push(message);
    }
  }

  // Only the field errors go in: an unassigned message is the caller's to show (a toast), and it must
  // not also become a form level error. That would render nowhere yet keep the form invalid, and then
  // pressing Save again on the unchanged form is dropped before onSubmit runs — the server is never
  // asked again and the toast, the only sign of the refusal, never comes back. Leaving the form valid
  // lets the resubmit go through and the message reappear (see useEntityEditForm), which is what a user
  // re-pressing Save is asking for. A field error still blocks its own field until edited, as it should.
  setServerErrors(form, { onServer: { fields } });

  return { unassigned, hasAssigned: Object.keys(fields).length > 0 };
}

/** Clears a previous round of server errors, e.g. before the next submit. */
export function clearServerValidationErrors(form: ErrorMapTarget): void {
  setServerErrors(form, { onServer: { fields: {} } });
}
