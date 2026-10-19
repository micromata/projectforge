import type { ValidationError } from "@/lib/rs/types";

/**
 * The server's complaint about a value of the wizard that lives in no form of its own — the parent
 * element, the name of a new group. The values inside the customer and project forms show theirs on
 * their fields instead (see applyServerValidationErrors).
 */
export function WizardFieldError({
  errors,
  fieldId,
}: {
  errors: ValidationError[];
  fieldId: string;
}) {
  const messages = errors
    .filter((error) => error.fieldId === fieldId)
    .map((error) => error.message);
  if (messages.length === 0) return null;
  return (
    <p role="alert" className="text-xs text-destructive">
      {messages.join(" ")}
    </p>
  );
}
