"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import { StarIcon } from "@hugeicons/core-free-icons";
import { cn } from "@/lib/utils";
import {
  FieldShell,
  useFieldIds,
  type BaseFieldProps,
  type FieldMetaState,
} from "./field-shell";
import { useEntityEditForm, useFormReadOnly } from "./form-context";
import { useFieldErrors } from "./use-field-errors";

export interface RatingFieldProps extends BaseFieldProps {
  /**
   * The text of every level, from "not rated" (0) up to the highest — as many stars as there are levels
   * above zero. Shown beside the stars for the current value and as the tooltip of each star.
   */
  levels: readonly string[];
  disabled?: boolean;
}

/**
 * A rating of a few stars — the `UIRatingStars` of the legacy React form (the skill matrix's self-rating
 * and interest). A click on a star sets its level, a click on the star of the current level clears the
 * rating back to 0, as React did. The value is a number, 0 for none, never null.
 */
export function RatingField({
  name,
  label,
  hint,
  className,
  levels,
  disabled,
}: RatingFieldProps) {
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  const formReadOnly = useFormReadOnly();
  const readOnly = disabled || formReadOnly;
  const max = levels.length - 1;
  return (
    <form.Field name={name as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as FieldMetaState;
        const invalid = meta.isTouched && !meta.isValid;
        const value = Math.max(
          0,
          Math.min(Number(field.state.value) || 0, max)
        );
        return (
          <FieldShell
            name={name}
            label={label}
            readOnly={readOnly}
            hint={hint}
            invalid={invalid}
            errors={fieldErrors(meta, label)}
            className={className}
            ids={ids}
          >
            <div className="flex min-h-7 items-center gap-2">
              <div
                id={ids.controlId}
                role="radiogroup"
                aria-labelledby={ids.labelId}
                className="inline-flex items-center gap-0.5"
              >
                {Array.from({ length: max }, (_, index) => {
                  const level = index + 1;
                  const filled = level <= value;
                  return (
                    <button
                      key={level}
                      type="button"
                      role="radio"
                      aria-checked={level === value}
                      aria-label={levels[level]}
                      title={levels[level]}
                      disabled={readOnly}
                      onClick={() => {
                        field.handleChange(level === value ? 0 : level);
                        field.handleBlur();
                      }}
                      className={cn(
                        "rounded-sm p-0.5 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring",
                        filled
                          ? "text-brand-yellow"
                          : "text-muted-foreground/50",
                        !readOnly && "cursor-pointer hover:text-brand-yellow"
                      )}
                    >
                      <HugeiconsIcon
                        icon={StarIcon}
                        size={18}
                        fill={filled ? "currentColor" : "none"}
                      />
                    </button>
                  );
                })}
              </div>
              <span className="text-sm text-muted-foreground">
                {levels[value]}
              </span>
            </div>
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
