"use client";

import { RichTextEditor } from "@/components/shared/rich-text-editor";
import {
  FieldShell,
  useFieldIds,
  type BaseFieldProps,
  type FieldMetaState,
} from "./field-shell";
import { useEntityEditForm, useFieldMetadata } from "./form-context";
import { useFieldErrors } from "./use-field-errors";

export interface RichTextFieldProps extends BaseFieldProps {
  /** Shown but not editable — a value this user may read and not change. */
  disabled?: boolean;
  placeholder?: string;
  /** The entity has no metadata for this field (see TextAreaField). */
  metadataLess?: boolean;
}

/**
 * A form field holding rich text (the HTML of [RichTextEditor]), the counterpart of TextAreaField for a
 * value rendered with [RichText]. An emptied editor is null unless the field is required.
 */
export function RichTextField({
  name,
  label,
  hint,
  className,
  disabled,
  placeholder,
  metadataLess,
}: RichTextFieldProps) {
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  const { required } = useFieldMetadata(name, metadataLess);
  return (
    <form.Field name={name as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as FieldMetaState;
        const invalid = meta.isTouched && !meta.isValid;
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
            <RichTextEditor
              id={ids.controlId}
              value={field.state.value as string | null}
              disabled={disabled}
              placeholder={placeholder}
              invalid={invalid}
              // Same null-vs-"" rule as TextAreaField.
              onChange={(html) =>
                field.handleChange(required ? html : html || null)
              }
              onBlur={field.handleBlur}
            />
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
