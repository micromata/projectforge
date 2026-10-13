import type { ValidationError } from "@/lib/rs/types";
import type { ChangePasswordField } from "./types";

const FIELDS: readonly ChangePasswordField[] = [
  "loginPassword",
  "newPassword",
  "passwordRepeat",
];

export type FieldErrors = Partial<Record<ChangePasswordField, string>>;

/**
 * Splits the backend's validation errors into the ones of a field (several joined) and the others, which
 * the caller shows as toasts. The backend translates every message (ValidationError.create).
 */
export function splitErrors(errors: ValidationError[]): {
  fieldErrors: FieldErrors;
  unassigned: string[];
} {
  const fieldErrors: FieldErrors = {};
  const unassigned: string[] = [];
  for (const error of errors) {
    const message = error.message?.trim();
    if (!message) continue;
    const field = FIELDS.find((f) => f === error.fieldId);
    if (field) {
      fieldErrors[field] = fieldErrors[field]
        ? `${fieldErrors[field]}. ${message}`
        : message;
    } else {
      unassigned.push(message);
    }
  }
  return { fieldErrors, unassigned };
}
