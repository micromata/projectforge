"use client";

import { useStore } from "@tanstack/react-form";
import { ValueCombobox } from "@/components/shared/value-combobox";
import {
  FieldShell,
  useFieldIds,
  type BaseFieldProps,
  type FieldMetaState,
} from "@/components/shared/form/field-shell";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { useFieldErrors } from "@/components/shared/form/use-field-errors";
import { useFormatContext } from "@/hooks/use-format";
import { compareText } from "@/lib/format";
import type { CustomerGroupsValues } from "./types";

/**
 * The customer groups of a business unit, picked from the groups of this very form — so a group added
 * a moment ago can be assigned before anything is saved (its key is assigned on adding, see [newKey]).
 * A group without a name yet is not offered: there would be nothing to recognise it by.
 *
 * Offered and shown alphabetically; the stored order of the keys means nothing.
 */
export function GroupsSelectField({
  name,
  label,
  hint,
  className,
}: BaseFieldProps) {
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  const format = useFormatContext();
  const groups = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (s: any) => (s.values as CustomerGroupsValues).groups
  );
  const options = groups
    .filter((group) => group.name.trim())
    .map((group) => ({ value: group.key, label: group.name.trim() }))
    .sort((a, b) => compareText(a.label, b.label, format));
  return (
    <form.Field name={name as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as FieldMetaState;
        return (
          <FieldShell
            name={name}
            label={label}
            hint={hint}
            invalid={meta.isTouched && !meta.isValid}
            errors={fieldErrors(meta, label)}
            className={className}
            ids={ids}
          >
            <ValueCombobox
              id={ids.controlId}
              aria-label={label}
              multi
              options={options}
              // In the options' order, so the chips read alphabetically too; a group removed above is
              // dropped here as well, as it is on saving (see toPayload).
              selected={options
                .map((option) => option.value)
                .filter((key) =>
                  ((field.state.value as string[] | null) ?? []).includes(key)
                )}
              onChange={(next) => {
                field.handleChange(next);
                field.handleBlur();
              }}
            />
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
