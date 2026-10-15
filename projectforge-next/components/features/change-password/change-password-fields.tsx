"use client";

import { FieldShell, useFieldIds } from "@/components/shared/form/field-shell";
import { SecretInput } from "@/components/shared/secret-input";
import type { ChangePasswordField } from "./types";

export interface PasswordProps {
  name: ChangePasswordField;
  label: string;
  value: string;
  error?: string;
  onChange: (value: string) => void;
  hint?: string;
  autoComplete?: string;
  autoFocus?: boolean;
}

/** A required password box with its label, the backend's error below it. */
export function PasswordField({
  name,
  label,
  value,
  error,
  onChange,
  hint,
  autoComplete,
  autoFocus,
}: PasswordProps) {
  const ids = useFieldIds();
  return (
    <FieldShell
      name={name}
      label={label}
      hint={hint}
      required
      invalid={!!error}
      errors={error ? [error] : []}
      ids={ids}
    >
      <SecretInput
        id={ids.controlId}
        label={label}
        value={value}
        onChange={onChange}
        invalid={!!error}
        copyable={false}
        autoComplete={autoComplete}
        autoFocus={autoFocus}
      />
    </FieldShell>
  );
}
