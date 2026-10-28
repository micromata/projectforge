"use client";

import type { ReactNode } from "react";
import { FieldShell, useFieldIds } from "@/components/shared/form/field-shell";

/** A labelled control of the variable dialog, which holds its values itself (no form of the page). */
export function MerlinDialogField({
  label,
  hint,
  children,
}: {
  label: string;
  hint?: string;
  children: (id: string) => ReactNode;
}) {
  const ids = useFieldIds();
  return (
    <FieldShell label={label} hint={hint} invalid={false} errors={[]} ids={ids}>
      {children(ids.controlId)}
    </FieldShell>
  );
}
