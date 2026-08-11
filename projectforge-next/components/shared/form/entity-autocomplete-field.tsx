"use client";

import {
  EntityAutocomplete,
  type EntityRef,
} from "@/components/shared/entity-autocomplete";
import {
  FieldShell,
  useFieldIds,
  type BaseFieldProps,
  type FieldMetaState,
} from "./field-shell";
import { useEntityEditForm, useFieldMetadata } from "./form-context";
import { useFieldErrors } from "./use-field-errors";

export interface EntityAutocompleteFieldProps extends BaseFieldProps {
  /**
   * REST category to search in — `user`, `task`, `project`, `customer`. The lookup url is that
   * category's `autosearch` (see `AutoCompletion.getAutoCompletionUrl` in projectforge-rest), which is
   * built here rather than declared, so a page cannot point a field at the wrong entity's search.
   */
  entity: string;
  /** Characters before the lookup fires; the backend defaults it to 2. */
  minChars?: number;
  /** Called after the value changed, for a field that fills others from it (project → customer). */
  onPicked?: (value: EntityRef | null) => void;
  /**
   * The entity has no metadata for this field, and cannot have any: `KundeDO` and `ProjektDO` have no
   * `UIDataType`, so an order's customer and project are absent from the generated metadata however
   * they are annotated. Says so instead of letting the dev warning cry drift (see [useFieldMetadata]).
   */
  metadataLess?: boolean;
}

/**
 * Picks a referenced entity — the contact person of an order, the task of a position — bound to a form
 * value holding the reference itself (`{id, displayName}`, what the DTO carries and what
 * `BaseDTO.copyTo` resolves back by id).
 *
 * The picker itself is the shared [EntityAutocomplete], which the filter row uses too; this adds the
 * label, the errors and the binding.
 */
export function EntityAutocompleteField({
  name,
  label,
  hint,
  className,
  entity,
  minChars,
  onPicked,
  metadataLess,
}: EntityAutocompleteFieldProps) {
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
            label={label}
            required={required}
            hint={hint}
            invalid={invalid}
            errors={fieldErrors(meta, label)}
            className={className}
            ids={ids}
          >
            <EntityAutocomplete
              id={ids.controlId}
              // The trigger is a button, which a <label htmlFor> cannot name — same as SelectField.
              aria-label={label}
              url={`${entity}/autosearch?search=:search`}
              value={(field.state.value as EntityRef | null) ?? null}
              minChars={minChars}
              onChange={(value) => {
                field.handleChange(value);
                // Blurring by hand: the picker is a popover, so nothing else ever marks the field
                // touched and its error would stay hidden.
                field.handleBlur();
                onPicked?.(value);
              }}
            />
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
