"use client";

import { FieldHint } from "@/components/shared/form/field-hint";

/** The header of a column of a row editor (holidays, BWA): its label with the explanation behind an icon. */
export function ColumnHeader({ label, hint }: { label: string; hint?: string }) {
  return (
    <span className="flex min-w-0 items-center gap-1">
      <span className="truncate">{label}</span>
      {hint && <FieldHint hint={hint} label={label} />}
    </span>
  );
}
