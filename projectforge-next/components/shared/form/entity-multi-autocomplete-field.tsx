"use client";

import { useTranslations } from "next-intl";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
import { EntityMultiAutocomplete } from "@/components/shared/entity-multi-autocomplete";
import {
  FieldShell,
  useFieldIds,
  type BaseFieldProps,
  type FieldMetaState,
} from "./field-shell";
import { useEntityEditForm, useFieldMetadata } from "./form-context";
import { useFieldErrors } from "./use-field-errors";

export interface EntityMultiAutocompleteFieldProps extends BaseFieldProps {
  /**
   * REST category to search in — `user`, `group`, `employee`. As in [EntityAutocompleteField] the
   * lookup url is that category's `autosearch`, built here rather than declared.
   */
  entity: string;
  /** Characters before the lookup fires; the backend defaults it to 2. */
  minChars?: number;
  /** Further request parameters of that search, see [EntitySearchList]. */
  params?: Record<string, unknown>;
  /**
   * The entity has no metadata for this field, and cannot have any: a collection is no
   * `@PropertyInfo` field of the DO, so a group's `assignedUsers` is absent from the generated
   * metadata however it is annotated (see [useFieldMetadata]).
   */
  metadataLess?: boolean;
}

/**
 * Picks any number of referenced entities — the members of a group, bound to a form value holding the
 * references themselves (`[{id, displayName}, …]`, what the DTO carries and what `copyTo` resolves back
 * by id).
 *
 * The picker is [EntityMultiAutocomplete]; this adds the label, the errors and the binding.
 */
export function EntityMultiAutocompleteField({
  name,
  label,
  hint,
  className,
  entity,
  minChars,
  params,
  metadataLess,
}: EntityMultiAutocompleteFieldProps) {
  const t = useTranslations();
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
            <EntityMultiAutocomplete
              id={ids.controlId}
              // The trigger is a button, which a <label htmlFor> cannot name — same as SelectField.
              aria-label={label}
              url={`${entity}/autosearch?search=:search`}
              value={(field.state.value as EntityRef[] | null) ?? []}
              onChange={(next) => {
                field.handleChange(next);
                // Blurring by hand: the picker is a popover, so nothing else ever marks the field
                // touched and its error would stay hidden (as in [EntityAutocompleteField]).
                field.handleBlur();
              }}
              removeLabel={(entry) => `${t("delete")}: ${entry.displayName}`}
              minChars={minChars}
              params={params}
            />
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
