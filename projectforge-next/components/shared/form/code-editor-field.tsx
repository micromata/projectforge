"use client";

import {
  CodeEditor,
  type CodeEditorProps,
} from "@/components/shared/code-editor/code-editor";
import {
  FieldShell,
  useFieldIds,
  type BaseFieldProps,
  type FieldMetaState,
} from "./field-shell";
import { useEntityEditForm, useFieldMetadata } from "./form-context";
import { useFieldErrors } from "./use-field-errors";

export interface CodeEditorFieldProps extends BaseFieldProps {
  language?: CodeEditorProps["language"];
  minHeight?: string;
  /** Shown but not editable (see DeclaredField.readOnly). */
  disabled?: boolean;
  /** The entity has no metadata for this field (see [useFieldMetadata]). */
  metadataLess?: boolean;
}

/** Source code bound to a form value, in the [CodeEditor]. An emptied editor is null. */
export function CodeEditorField({
  name,
  label,
  hint,
  className,
  language,
  minHeight,
  disabled,
  metadataLess,
}: CodeEditorFieldProps) {
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  const { required } = useFieldMetadata(name, metadataLess);
  return (
    <form.Field name={name as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as FieldMetaState;
        return (
          <FieldShell
            name={name}
            label={label}
            required={required}
            readOnly={disabled}
            hint={hint}
            invalid={meta.isTouched && !meta.isValid}
            errors={fieldErrors(meta, label)}
            className={className}
            ids={ids}
          >
            <CodeEditor
              id={ids.controlId}
              ariaLabel={label}
              value={(field.state.value as string | null) ?? ""}
              onChange={(next) =>
                field.handleChange(required ? next : next || null)
              }
              onBlur={field.handleBlur}
              language={language}
              minHeight={minHeight}
              readOnly={disabled}
            />
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
