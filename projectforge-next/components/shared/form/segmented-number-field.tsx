"use client";

import { useCallback, useRef } from "react";
import { useStore } from "@tanstack/react-form";
import { FieldDescription, FieldError } from "@/components/ui/field";
import {
  splitPastedSegments,
  type NumberSegment,
} from "@/lib/form/number-segments";
import { cn } from "@/lib/utils";
import { useEntityEditForm, useEntityMetadata } from "./form-context";
import { NumberSegmentInput } from "./number-segment-input";
import { useFieldErrors, type FieldErrorMeta } from "./use-field-errors";

export interface SegmentedNumberFieldProps {
  /** Name of the whole group, e.g. the translation of `fibu.kost.kostentraeger`. */
  label: string;
  segments: NumberSegment[];
  /** Rendered between the boxes, e.g. "." for a cost number. Decorative, hence `aria-hidden`. */
  separator?: string;
  hint?: string;
  className?: string;
}

/**
 * A number made of several bounded parts, each of which is a field of its own.
 *
 * One `form.Field` per segment rather than one composite value: the DTO has one property per part, and
 * a server side complaint carrying `causedByField = "bereich"` has to land under that box (see
 * `applyServerValidationErrors`). The parts share a `<legend>` and one error line, because to the user
 * they are a single number — four identical complaints under one number would be noise.
 *
 * The bounds come from the caller (see `NumberSegment`), not from the metadata: a column length is a
 * digit count, not a maximum, and the authority stays the entity's own check.
 */
export function SegmentedNumberField({
  label,
  segments,
  separator,
  hint,
  className,
}: SegmentedNumberFieldProps) {
  const form = useEntityEditForm();
  const metadata = useEntityMetadata();
  const fieldErrors = useFieldErrors();
  const inputs = useRef<(HTMLInputElement | null)[]>([]);

  const focus = useCallback((index: number) => {
    const input = inputs.current[index];
    if (!input) return;
    input.focus();
    input.select();
  }, []);

  const applyPaste = useCallback(
    (text: string) => {
      const values = splitPastedSegments(text, segments);
      values.forEach((value, name) => form.setFieldValue(name, value));
      focus(Math.min(values.size, segments.length) - 1);
    },
    [focus, form, segments]
  );

  const required = segments.some((s) => metadata.fields[s.name]?.required);
  // `useStore` on the form's store, not the (nonexistent) `form.useStore`: the group needs the meta
  // of *all* its segments at once to decide whether to show one shared error line, and subscribing
  // per segment through `form.Field` would only give each box its own.
  const metas = useStore(form.store, (state) =>
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    segments.map((s) => (state as any).fieldMeta[s.name])
  ) as (FieldErrorMeta & { isTouched?: boolean; isValid?: boolean })[];

  const invalid =
    metas.some((m) => m?.isTouched) &&
    metas.some((m) => m && m.isValid === false);
  // Deduplicated: two empty segments would otherwise repeat the same sentence.
  const errors = [
    ...new Set(metas.flatMap((m) => (m ? fieldErrors(m, label) : []))),
  ];

  return (
    <fieldset
      data-invalid={invalid || undefined}
      className={cn("flex flex-col gap-1.5", className)}
    >
      <legend className="text-[11.5px] font-semibold uppercase tracking-wide text-muted-foreground">
        {label}
        {required && <span className="ml-0.5 text-primary">*</span>}
      </legend>
      <div className="flex items-center gap-1">
        {segments.map((segment, index) => (
          <div key={segment.name} className="flex items-center gap-1">
            {index > 0 && separator && (
              <span aria-hidden className="text-muted-foreground">
                {separator}
              </span>
            )}
            <NumberSegmentInput
              segment={segment}
              ariaLabel={`${label}: ${segment.label}`}
              invalid={invalid}
              onFilled={() => focus(index + 1)}
              onLeave={(direction) =>
                focus(direction === "prev" ? index - 1 : index + 1)
              }
              registerInput={(el) => {
                inputs.current[index] = el;
              }}
              onPasteSegments={applyPaste}
            />
          </div>
        ))}
      </div>
      {hint && !invalid && <FieldDescription>{hint}</FieldDescription>}
      {invalid && errors.length > 0 && (
        <FieldError>{errors.join(". ")}</FieldError>
      )}
    </fieldset>
  );
}
