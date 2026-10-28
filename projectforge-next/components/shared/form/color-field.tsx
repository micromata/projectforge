"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import { Delete02Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { ColorSwatchPopover } from "@/components/shared/color-swatch-popover";
import {
  FieldShell,
  useFieldIds,
  type BaseFieldProps,
  type FieldMetaState,
} from "./field-shell";
import { useEntityEditForm, useFieldMetadata } from "./form-context";
import { useFieldErrors } from "./use-field-errors";

/**
 * An optional colour as hex code (`#ffcb00`): the swatch opens the shared picker, the button beside it
 * removes the colour again (stored as `""`).
 */
export function ColorField({
  name,
  label,
  hint,
  className,
  removeLabel,
}: BaseFieldProps & { removeLabel: string }) {
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  const { required } = useFieldMetadata(name);
  return (
    <form.Field name={name as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as FieldMetaState;
        const value = (field.state.value as string | null) ?? "";
        return (
          <FieldShell
            name={name}
            label={label}
            required={required}
            hint={hint}
            invalid={meta.isTouched && !meta.isValid}
            errors={fieldErrors(meta, label)}
            className={className}
            ids={ids}
          >
            <div className="flex h-9 items-center gap-2">
              <ColorSwatchPopover
                value={value}
                onChange={(color) => {
                  field.handleChange(color);
                  field.handleBlur();
                }}
                aria-label={label}
              />
              <span className="text-sm text-muted-foreground">{value}</span>
              {value && (
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="size-7"
                  aria-label={removeLabel}
                  onClick={() => field.handleChange("")}
                >
                  <HugeiconsIcon icon={Delete02Icon} className="h-4 w-4" />
                </Button>
              )}
            </div>
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
