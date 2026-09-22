"use client";

import { Checkbox } from "@/components/ui/checkbox";
import { Field, FieldError } from "@/components/ui/field";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";
import { FieldHint } from "./field-hint";
import {
  useFieldIds,
  type BaseFieldProps,
  type FieldMetaState,
} from "./field-shell";
import { useEntityEditForm } from "./form-context";
import { useFieldErrors } from "./use-field-errors";

export interface CheckboxFieldProps extends BaseFieldProps {
  disabled?: boolean;
}

/**
 * A boolean, with its label beside the box rather than above it.
 *
 * Hence not [FieldShell], which stacks label over control: a checkbox reads as one line ("☑ fully
 * invoiced"), and a label above an empty box a row wide reads as a heading for something missing.
 *
 * The value is never null: a `Boolean` column of the backend is a primitive there
 * (`AuftragsPositionDO.vollstaendigFakturiert`), and a tri-state checkbox would offer a value the
 * entity cannot hold.
 */
export function CheckboxField({
  name,
  label,
  hint,
  className,
  disabled,
}: CheckboxFieldProps) {
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  return (
    <form.Field name={name as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as FieldMetaState;
        const invalid = meta.isTouched && !meta.isValid;
        const errors = fieldErrors(meta, label);
        return (
          <Field
            data-field={name}
            data-invalid={invalid || undefined}
            className={cn("gap-1.5", className)}
          >
            <div className="flex items-center gap-2">
              <Checkbox
                id={ids.controlId}
                checked={field.state.value === true}
                disabled={disabled}
                aria-invalid={invalid || undefined}
                onCheckedChange={(value) => field.handleChange(value === true)}
                onBlur={field.handleBlur}
              />
              <Label
                htmlFor={ids.controlId}
                className="text-xs font-normal text-foreground"
              >
                {label}
              </Label>
              {/* Outside the <label>, which would forward a click on it to the box — see FieldShell. */}
              {hint && <FieldHint hint={hint} label={label} />}
            </div>
            {invalid && errors.length > 0 && (
              <FieldError>{errors.join(". ")}</FieldError>
            )}
          </Field>
        );
      }}
    </form.Field>
  );
}
