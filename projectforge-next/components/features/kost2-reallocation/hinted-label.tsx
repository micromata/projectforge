"use client";

import { FieldHint } from "@/components/shared/form/field-hint";
import { Label } from "@/components/ui/label";

/** A field label with the explanation of the field behind an info icon. */
export function HintedLabel({
  label,
  hint,
  htmlFor,
}: {
  label: string;
  hint: string;
  htmlFor?: string;
}) {
  return (
    <div className="flex items-center gap-1.5">
      <Label htmlFor={htmlFor}>{label}</Label>
      <FieldHint hint={hint} label={label} />
    </div>
  );
}

/**
 * A section heading with its explanation. The icon sits beside the heading, not in it, so the heading keeps its
 * plain name.
 */
export function HintedHeading({
  label,
  hint,
}: {
  label: string;
  hint: string;
}) {
  return (
    <div className="flex items-center gap-1.5">
      <h3 className="text-sm font-semibold">{label}</h3>
      <FieldHint hint={hint} label={label} />
    </div>
  );
}
