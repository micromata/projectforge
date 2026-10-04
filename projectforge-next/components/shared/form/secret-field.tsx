"use client";

import type { ReactNode } from "react";
import { SecretInput } from "@/components/shared/secret-input";
import {
  FieldShell,
  useFieldIds,
  type BaseFieldProps,
  type FieldMetaState,
} from "./field-shell";
import { useEntityEditForm, useFieldMetadata } from "./form-context";
import { useFieldErrors } from "./use-field-errors";

export interface SecretFieldProps extends BaseFieldProps {
  disabled?: boolean;
  /** The beginning of the hidden secret shows through (see SecretInput). */
  peek?: boolean;
  /** Further controls after the copy button — a data transfer password's "renew". */
  children?: ReactNode;
}

/**
 * A form value that is a secret — a password, an access token: InputField's binding (metadata,
 * errors, the null-vs-"" rule) around a [SecretInput], so it is hidden until revealed and can be
 * copied as it is.
 */
export function SecretField({
  name,
  label,
  hint,
  className,
  disabled,
  peek,
  children,
}: SecretFieldProps) {
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
            <SecretInput
              id={ids.controlId}
              label={label}
              value={field.state.value as string | null}
              invalid={invalid}
              disabled={disabled}
              maxLength={maxLength}
              peek={peek}
              // As InputField: an emptied optional value is null, a required one stays "".
              onChange={(next) =>
                field.handleChange(required ? next : next || null)
              }
              onBlur={field.handleBlur}
            >
              {children}
            </SecretInput>
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
