"use client";

import { SuggestInput } from "@/components/shared/suggest-input";
import {
  FieldShell,
  useFieldIds,
  type BaseFieldProps,
  type FieldMetaState,
} from "./field-shell";
import { useEntityEditForm, useFieldMetadata } from "./form-context";
import { useFieldErrors } from "./use-field-errors";

export interface StringSuggestFieldProps extends BaseFieldProps {
  /** See [SuggestInputProps.suggest] — the values the backend has already seen for this field. */
  suggest: (search: string, signal?: AbortSignal) => Promise<string[]>;
  /** See [SuggestInputProps.queryKey] — everything the lookup depends on besides the term. */
  queryKey: readonly unknown[];
  disabled?: boolean;
}

/**
 * A free-text field of a hand-built form that completes itself from what the backend has already seen —
 * a time sheet's location and its reference.
 *
 * Not an [EntityAutocompleteField]: that binds a `{id, displayName}` reference and accepts nothing else,
 * while the value here is the string itself and any string is valid. Everything the two have in common —
 * the label, the ⓘ, the error line, `required` and `maxLength` from the metadata — comes from the same
 * [FieldShell].
 */
export function StringSuggestField({
  name,
  label,
  hint,
  className,
  suggest,
  queryKey,
  disabled,
}: StringSuggestFieldProps) {
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
            <SuggestInput
              id={ids.controlId}
              value={(field.state.value as string | null) ?? ""}
              // Same null-vs-"" rule as [InputField]: an emptied optional field becomes null, which is
              // how the backend stores "no value", while a required one keeps "".
              onChange={(next) =>
                field.handleChange(required ? next : next || null)
              }
              onBlur={field.handleBlur}
              suggest={suggest}
              queryKey={queryKey}
              invalid={invalid}
              disabled={disabled}
              required={required}
              maxLength={maxLength}
            />
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
