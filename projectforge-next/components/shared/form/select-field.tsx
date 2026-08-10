"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Cancel01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { cn } from "@/lib/utils";
import type { SelectOption } from "@/lib/validation/from-metadata";
import {
  FieldShell,
  useFieldIds,
  type BaseFieldProps,
  type FieldMetaState,
} from "./field-shell";
import { useEntityEditForm, useFieldMetadata } from "./form-context";
import { useFieldErrors } from "./use-field-errors";

export interface SelectFieldProps extends BaseFieldProps {
  options: SelectOption[];
  /**
   * Offers a button that sets the field back to null, matching the ✕ of the legacy page. Radix has no
   * such affordance of its own: an empty SelectItem value is forbidden, so "no value" is unreachable
   * once one is set.
   *
   * Defaults to whether the field may be null at all, i.e. to `!required` from the metadata — so
   * BookDO's `type` (`nullable = true`) can be cleared and its `status` cannot, without either
   * decision being made here.
   */
  clearable?: boolean;
  /**
   * Renders the value larger and in the accent colour. For the one value a reader looks for first —
   * the status of a book, the status of a cost unit — which the legacy pages buried among the others.
   */
  emphasized?: boolean;
}

export function SelectField({
  name,
  label,
  hint,
  className,
  options,
  clearable,
  emphasized,
}: SelectFieldProps) {
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  const tCommon = useTranslations();
  const { required } = useFieldMetadata(name);
  const canClear = clearable ?? !required;
  return (
    <form.Field name={name as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as FieldMetaState;
        const invalid = meta.isTouched && !meta.isValid;
        const raw = (field.state.value as string | null) ?? "";
        return (
          <FieldShell
            label={label}
            required={required}
            hint={hint}
            invalid={invalid}
            errors={fieldErrors(meta, label)}
            className={className}
            ids={ids}
          >
            <div className="flex items-center gap-1">
              <Select
                value={raw}
                // "" is never a choice a user can make — Radix forbids an empty SelectItem value —
                // so it can only come from its own hidden native <select> (SelectBubbleInput): that
                // mirrors every value change into the native element and dispatches a change event,
                // which comes back through here. Its <option>s exist only while SelectContent is
                // mounted, i.e. while the dropdown is open, so setting the value of a *closed*
                // select matches nothing, leaves it at "" and would wipe the field. This happens
                // whenever the value changes without the dropdown being opened — for us when the
                // loaded entity resets the form (see useEntityEditForm) to something other than the
                // default.
                onValueChange={(v) => {
                  if (v === "") return;
                  field.handleChange(v);
                }}
              >
                {/* The trigger is a button, which a <label htmlFor> cannot name — hence labelledby. */}
                <SelectTrigger
                  id={ids.controlId}
                  aria-labelledby={ids.labelId}
                  className={cn(
                    "flex-1",
                    emphasized &&
                      "h-9 border-primary/40 bg-primary/5 text-sm font-semibold text-primary data-[size=default]:h-9"
                  )}
                >
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {options.map((o) => (
                    <SelectItem key={o.value} value={o.value}>
                      {o.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {canClear && raw !== "" && (
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="size-7 shrink-0 text-muted-foreground"
                  aria-label={`${tCommon("reset")}: ${label}`}
                  onClick={() => field.handleChange(null)}
                >
                  <HugeiconsIcon icon={Cancel01Icon} size={13} />
                </Button>
              )}
            </div>
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
