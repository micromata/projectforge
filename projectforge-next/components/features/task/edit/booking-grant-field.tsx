"use client";

import type { ReactNode } from "react";
import {
  FieldShell,
  useFieldIds,
  type FieldIds,
} from "@/components/shared/form/field-shell";

/**
 * A labelled field of the booking grant dialog. No field errors: the backend's refusal is one message, shown
 * above the fields.
 */
export function BookingGrantField({
  label,
  required,
  children,
}: {
  label: string;
  required?: boolean;
  children: (ids: FieldIds) => ReactNode;
}) {
  const ids = useFieldIds();
  return (
    <FieldShell
      label={label}
      required={required}
      invalid={false}
      errors={[]}
      ids={ids}
    >
      {children(ids)}
    </FieldShell>
  );
}
