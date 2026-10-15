"use client";

import type { ReactNode } from "react";
import { FieldShell, useFieldIds } from "./field-shell";

/**
 * A value shown, not edited (a user name administered elsewhere, the date of the last change), with a
 * label like the fields' — and a dash where there is none.
 */
export function ReadOnlyValue({
  label,
  hint,
  className,
  children,
}: {
  label: string;
  hint?: ReactNode;
  className?: string;
  children: ReactNode;
}) {
  const ids = useFieldIds();
  return (
    <FieldShell
      label={label}
      hint={hint}
      readOnly
      invalid={false}
      errors={[]}
      ids={ids}
      className={className ?? "min-w-0"}
    >
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
