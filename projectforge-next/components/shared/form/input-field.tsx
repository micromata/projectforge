"use client";

import { Input } from "@/components/ui/input";
import { DateInput } from "@/components/shared/date-input";
import {
  FieldShell,
  useFieldIds,
  type BaseFieldProps,
  type FieldMetaState,
} from "./field-shell";
import { useEntityEditForm, useFieldMetadata } from "./form-context";
import { useFieldErrors } from "./use-field-errors";

export interface InputFieldProps extends BaseFieldProps {
  /** `date` is a `LocalDate` and goes through the shared [DateInput], never a native date field. */
  type?: "text" | "date";
  placeholder?: string;
  /** Shown but not editable — a value this user may read and not change (see DeclaredField.readOnly). */
  disabled?: boolean;
}

export function InputField({
  name,
  label,
  hint,
  className,
  type = "text",
  placeholder,
  disabled,
}: InputFieldProps) {
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  const { required, maxLength } = useFieldMetadata(name);
  return (
    <form.Field name={name as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as FieldMetaState;
        const invalid = meta.isTouched && !meta.isValid;
        const raw = field.state.value as string | null;
        return (
          <FieldShell
            name={name}
            label={label}
            required={required}
            readOnly={disabled}
            hint={hint}
            invalid={invalid}
            errors={fieldErrors(meta, label)}
            className={className}
            ids={ids}
          >
            {type === "date" ? (
              <DateInput
                id={ids.controlId}
                value={raw}
                invalid={invalid}
                required={required}
                disabled={disabled}
                // Same null-vs-"" rule as below.
                onChange={(next) =>
                  field.handleChange(required ? (next ?? "") : next)
                }
                onBlur={field.handleBlur}
              />
            ) : (
              <Input
                id={ids.controlId}
                type={type}
                placeholder={placeholder}
                disabled={disabled}
                // The column's length, so typing stops at the limit instead of only complaining
                // afterwards. The Zod rule stays as the net for a value that didn't come from typing
                // (a paste is truncated by the browser, but a programmatic change isn't).
                maxLength={maxLength}
                value={raw ?? ""}
                // An emptied optional field becomes null, which is how the backend stores "no value".
                // A required one keeps "" — its schema expects a string and would otherwise complain
                // about the type instead of the missing value (see requiredString).
                onChange={(e) =>
                  field.handleChange(
                    required ? e.target.value : e.target.value || null
                  )
                }
                onBlur={field.handleBlur}
              />
            )}
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
