"use client";

import type { EntityRef } from "@/components/shared/entity-autocomplete";
import {
  EntityOrTextAutocomplete,
  type EntityOrText,
} from "@/components/shared/entity-or-text-autocomplete";
import {
  FieldShell,
  useFieldIds,
  type BaseFieldProps,
  type FieldMetaState,
} from "./field-shell";
import { useEntityEditForm, useFieldMetadata } from "./form-context";
import { useFieldErrors } from "./use-field-errors";

export interface EntityOrTextFieldProps extends Omit<BaseFieldProps, "name"> {
  /** Form value holding the picked record (`{id, displayName}`) — an order's `customer`. */
  entityName: string;
  /** Form value holding the free text — an order's `kundeText`. */
  textName: string;
  /** REST category to search in (`customer`), see [EntityAutocompleteFieldProps.entity]. */
  entity: string;
  /** Called after a record was picked (not on a free text), for a field that fills others from it. */
  onPicked?: (value: EntityRef | null) => void;
  /** The record has no metadata, see [EntityAutocompleteFieldProps.metadataLess]. */
  metadataLess?: boolean;
  disabled?: boolean;
}

/**
 * One field for two form values that exclude each other: a record picked from the list, or a free text
 * where no record fits — the customer and the free-text customer of an order or an invoice.
 *
 * Picking one clears the other, so the form never holds both. A value that does (data saved before the
 * two were one field) shows the record: the backend drops the free text beside a customer on save anyway
 * (`OrderEntityRest.transformForDB`).
 */
export function EntityOrTextField({
  entityName,
  textName,
  label,
  hint,
  className,
  entity,
  onPicked,
  metadataLess,
  disabled,
}: EntityOrTextFieldProps) {
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  const { maxLength } = useFieldMetadata(textName);
  useFieldMetadata(entityName, metadataLess);
  return (
    <form.Field name={entityName as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(entityField: any) => (
        <form.Field name={textName as never}>
          {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
          {(textField: any) => {
            const entityMeta = entityField.state.meta as FieldMetaState;
            const textMeta = textField.state.meta as FieldMetaState;
            const invalid =
              (entityMeta.isTouched && !entityMeta.isValid) ||
              (textMeta.isTouched && !textMeta.isValid);
            const ref = entityField.state.value as EntityRef | null;
            const text = textField.state.value as string | null;
            const value: EntityOrText | null = ref
              ? { kind: "entity", ref }
              : text
                ? { kind: "text", text }
                : null;
            const change = (next: EntityOrText | null) => {
              entityField.handleChange(
                next?.kind === "entity" ? next.ref : null
              );
              textField.handleChange(next?.kind === "text" ? next.text : null);
              // Blurring by hand: the picker is a popover, so nothing else marks the fields touched.
              entityField.handleBlur();
              textField.handleBlur();
              if (next?.kind !== "text") onPicked?.(next?.ref ?? null);
            };
            return (
              <FieldShell
                // The record's name: what a server error on the customer and an e2e test address.
                name={entityName}
                label={label}
                readOnly={disabled}
                hint={hint}
                invalid={invalid}
                errors={[
                  ...fieldErrors(entityMeta, label),
                  ...fieldErrors(textMeta, label),
                ]}
                className={className}
                ids={ids}
              >
                <EntityOrTextAutocomplete
                  id={ids.controlId}
                  aria-label={label}
                  url={`${entity}/autosearch?search=:search`}
                  value={value}
                  maxLength={maxLength}
                  disabled={disabled}
                  onChange={change}
                />
              </FieldShell>
            );
          }}
        </form.Field>
      )}
    </form.Field>
  );
}
