"use client";

import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import type { SelectOption } from "@/lib/validation/from-metadata";
import {
  FieldShell,
  useFieldIds,
  type BaseFieldProps,
  type FieldMetaState,
} from "./field-shell";
import { useEntityEditForm } from "./form-context";
import { useFieldErrors } from "./use-field-errors";

export interface CheckboxListFieldProps extends BaseFieldProps {
  /** The values to choose from, in the order shown. */
  options: SelectOption[];
  disabled?: boolean;
}

/**
 * Any number of values of a short fixed list (the employee statuses a notification goes to), bound to
 * a list of strings: a checkbox per option. An empty list is a value of its own ("none chosen").
 *
 * The value keeps the order of [options], not the order of clicking, so a saved list reads the same
 * every time.
 */
export function CheckboxListField({
  name,
  label,
  hint,
  className,
  options,
  disabled,
}: CheckboxListFieldProps) {
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  return (
    <form.Field name={name as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as FieldMetaState;
        const selected = new Set<string>(field.state.value ?? []);
        const toggle = (value: string, checked: boolean) => {
          if (checked) selected.add(value);
          else selected.delete(value);
          field.handleChange(
            options.map((o) => o.value).filter((v) => selected.has(v))
          );
        };
        return (
          <FieldShell
            name={name}
            label={label}
            hint={hint}
            invalid={!meta.isValid}
            errors={fieldErrors(meta, label)}
            className={className}
            ids={ids}
          >
            <div
              id={ids.controlId}
              role="group"
              aria-labelledby={ids.labelId}
              className="flex flex-wrap gap-x-4 gap-y-1.5 py-1"
            >
              {options.map((option) => {
                const id = `${ids.controlId}-${option.value}`;
                return (
                  <div key={option.value} className="flex items-center gap-2">
                    <Checkbox
                      id={id}
                      checked={selected.has(option.value)}
                      disabled={disabled}
                      onCheckedChange={(v) => toggle(option.value, v === true)}
                      onBlur={field.handleBlur}
                    />
                    <Label htmlFor={id} className="text-xs font-normal">
                      {option.label}
                    </Label>
                  </div>
                );
              })}
            </div>
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
