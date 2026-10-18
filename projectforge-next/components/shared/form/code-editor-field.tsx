"use client";

import {
  CodeEditor,
  type CodeEditorProps,
} from "@/components/shared/code-editor/code-editor";
import type { CodeVersionSource } from "@/components/shared/code-editor/types";
import {
  FieldShell,
  useFieldIds,
  type BaseFieldProps,
  type FieldMetaState,
} from "./field-shell";
import {
  useEntityData,
  useEntityEditForm,
  useFieldMetadata,
} from "./form-context";
import { useFieldErrors } from "./use-field-errors";

export interface CodeEditorFieldProps extends BaseFieldProps {
  language?: CodeEditorProps["language"];
  minHeight?: string;
  /** See [CodeEditorProps.fill]. */
  fill?: boolean;
  /** See [FieldShell]'s `hideLabel`. */
  hideLabel?: boolean;
  /** Shown but not editable (see DeclaredField.readOnly). */
  disabled?: boolean;
  /** The entity has no metadata for this field (see [useFieldMetadata]). */
  metadataLess?: boolean;
  /**
   * Offer the changes against the stored code (CodeEditorProps.original), for an entity that is
   * stored already. The stored code is the loaded entity's ([useEntityData]), not the form's default
   * values: the form object of the context is a snapshot of @tanstack/react-form's, whose `options`
   * keep the empty defaults it was created with before the entity had loaded.
   */
  showDiff?: boolean;
  /** With [showDiff]: the former versions the diff can compare as well. */
  versions?: CodeVersionSource;
}

/** Source code bound to a form value, in the [CodeEditor]. An emptied editor is null. */
export function CodeEditorField({
  name,
  label,
  hint,
  className,
  language,
  minHeight,
  fill,
  hideLabel,
  disabled,
  metadataLess,
  showDiff,
  versions,
}: CodeEditorFieldProps) {
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  const { required } = useFieldMetadata(name, metadataLess);
  const stored = useEntityData<Record<string, unknown>>();
  const original =
    showDiff && stored?.id != null
      ? ((stored[name] as string | null | undefined) ?? "")
      : undefined;
  return (
    <form.Field name={name as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as FieldMetaState;
        return (
          <FieldShell
            name={name}
            label={label}
            hideLabel={hideLabel}
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
              fill={fill}
              readOnly={disabled}
              original={original}
              versions={original === undefined ? undefined : versions}
            />
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
