"use client";

import { useId, type ReactNode } from "react";
import {
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
} from "@/components/ui/field";
import { cn } from "@/lib/utils";

/**
 * What every field of a hand-built form takes.
 *
 * `required` and `maxLength` are deliberately not props: they are the backend's rules and come from
 * the generated metadata via [useFieldMetadata], the same source the Zod schema reads. A caller that
 * could set them would be a second place to maintain them — and the one that had drifted before.
 */
export interface BaseFieldProps {
  /** Name of the form value, which is also the key of its entry in the entity's metadata. */
  name: string;
  label: string;
  hint?: string;
  className?: string;
}

/**
 * Ids tying a label to its control, so the control has an accessible name.
 *
 * `htmlFor` alone would do for an input, but the select's trigger is a `<button>`, which a label
 * cannot label — hence `labelId` for its `aria-labelledby`.
 */
export interface FieldIds {
  controlId: string;
  labelId: string;
}

export function useFieldIds(): FieldIds {
  const id = useId();
  return { controlId: `${id}-control`, labelId: `${id}-label` };
}

/** The slice of a @tanstack/react-form field's meta state the shell needs. */
export interface FieldMetaState {
  isTouched: boolean;
  isValid: boolean;
  errors?: unknown[];
}

export function FieldShell({
  label,
  required,
  readOnly,
  hint,
  invalid,
  errors,
  className,
  ids,
  children,
}: {
  label: string;
  required?: boolean;
  /** Shown but not fillable — suppresses the asterisk, see below. */
  readOnly?: boolean;
  hint?: string;
  invalid: boolean;
  errors: string[];
  className?: string;
  ids: FieldIds;
  children: ReactNode;
}) {
  return (
    <Field
      data-invalid={invalid || undefined}
      className={cn("gap-1.5", className)}
    >
      <FieldLabel
        id={ids.labelId}
        htmlFor={ids.controlId}
        className="text-[11.5px] font-semibold uppercase tracking-wide text-muted-foreground"
      >
        {label}
        {/* Not on a field the user cannot fill in: a value only the backend supplies (an order's
            number) is mandatory in the database but never the reader's obligation. */}
        {required && !readOnly && (
          <span className="ml-0.5 text-primary">*</span>
        )}
      </FieldLabel>
      {children}
      {hint && !invalid && <FieldDescription>{hint}</FieldDescription>}
      {invalid && errors.length > 0 && (
        <FieldError>{errors.join(". ")}</FieldError>
      )}
    </Field>
  );
}
