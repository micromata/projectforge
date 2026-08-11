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
}

/**
 * True when the form has a field of this name, a row of a nested collection included: an error of the
 * order book arrives as `positionen[0].titel` (see `AuftragPagesRest.validateRows`), which is exactly
 * the name the row's field is bound to — so it is accepted as soon as the form holds the array it
 * indexes into. Checking the root rather than the whole path is what makes that possible: the form's
 * field *names* only exist once a row is rendered, while the errors arrive for all of them at once.
 */
function isKnown(fieldId: string, knownFields: readonly string[]): boolean {
  if (knownFields.includes(fieldId)) return true;
  const root = fieldId.match(/^([^[.]+)\[\d+]\./)?.[1];
  return !!root && knownFields.includes(root);
}

/**
 * @param knownFields Field names the form actually renders. An error for anything else would be
 * written into a field that never displays it, so it is reported back as unassigned instead.
 */
export function applyServerValidationErrors(
  form: ErrorMapTarget,
  errors: ValidationError[],
  knownFields: readonly string[]
): ServerValidationResult {
  const fields: Record<string, string> = {};
  const unassigned: string[] = [];

  for (const error of errors) {
    // The backend always translates the message before sending it (ValidationError.create).
    const message = error.message?.trim();
    if (!message) continue;
    if (error.fieldId && isKnown(error.fieldId, knownFields)) {
      // Several errors on one field: keep them all rather than let the last one win.
      fields[error.fieldId] = fields[error.fieldId]
        ? `${fields[error.fieldId]}. ${message}`
        : message;
    } else {
      unassigned.push(message);
    }
  }

  setServerErrors(form, {
    onServer: {
      fields,
      // A form level error would render nowhere in the current layout, so the caller shows the
      // unassigned messages instead; setting it anyway keeps the form invalid, which blocks a
      // resubmit of unchanged values.
      ...(unassigned.length > 0 ? { form: unassigned.join(". ") } : {}),
    },
  });

  return { unassigned };
}

/** Clears a previous round of server errors, e.g. before the next submit. */
export function clearServerValidationErrors(form: ErrorMapTarget): void {
  setServerErrors(form, { onServer: { fields: {} } });
}
