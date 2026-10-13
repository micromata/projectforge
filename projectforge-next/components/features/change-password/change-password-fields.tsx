"use client";

import type { ReactNode } from "react";
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

/** A value shown, not edited (the user, the date of the last change), with a label like the fields'. */
export function ReadOnlyValue({
  label,
  children,
}: {
  label: string;
  children: ReactNode;
}) {
  const ids = useFieldIds();
  return (
    <FieldShell label={label} readOnly invalid={false} errors={[]} ids={ids}>
      <p
        id={ids.controlId}
        aria-labelledby={ids.labelId}
        className="min-h-7 py-1 text-sm break-words"
      >
        {children || "–"}
      </p>
    </FieldShell>
  );
}
