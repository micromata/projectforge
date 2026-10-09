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
  placeholder?: string;
  /** The entity has no metadata for this field (see [useFieldMetadata]). */
  metadataLess?: boolean;
}

/**
 * A rich text (HTML) bound to a form value, in the [RichTextEditor]; render the stored value with
 * [RichText]. An emptied editor is null unless the field is required.
 */
export function RichTextField({
  name,
  label,
  hint,
  className,
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
            hint={hint}
            invalid={invalid}
            errors={fieldErrors(meta, label)}
            className={className}
            ids={ids}
          >
            <RichTextEditor
              id={ids.controlId}
              value={(field.state.value as string | null) ?? ""}
              onChange={(html) =>
                field.handleChange(required ? html : html || null)
              }
              onBlur={field.handleBlur}
              placeholder={placeholder}
              invalid={invalid}
            />
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
