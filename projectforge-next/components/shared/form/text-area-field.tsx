"use client";

import { Textarea } from "@/components/ui/textarea";
import {
  FieldShell,
  useFieldIds,
  type BaseFieldProps,
  type FieldMetaState,
} from "./field-shell";
import { useEntityEditForm, useFieldMetadata } from "./form-context";
import { useFieldErrors } from "./use-field-errors";

export interface TextAreaFieldProps extends BaseFieldProps {
  rows?: number;
  /** Shown but not editable — a value this user may read and not change (see DeclaredField.readOnly). */
  disabled?: boolean;
}

export function TextAreaField({
  name,
  label,
  hint,
  className,
  rows = 4,
  disabled,
}: TextAreaFieldProps) {
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
            label={label}
            required={required}
            readOnly={disabled}
            hint={hint}
            invalid={invalid}
            errors={fieldErrors(meta, label)}
            className={className}
            ids={ids}
          >
            <Textarea
              id={ids.controlId}
              rows={rows}
              disabled={disabled}
              // Same as InputField: the column's length.
              maxLength={maxLength}
              value={raw ?? ""}
              // Same null-vs-"" rule as InputField.
              onChange={(e) =>
                field.handleChange(
                  required ? e.target.value : e.target.value || null
                )
              }
              onBlur={field.handleBlur}
            />
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
