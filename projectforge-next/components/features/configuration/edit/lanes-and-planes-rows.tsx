"use client";

import type { ReactNode } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Delete02Icon, PlusSignIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { FieldHint } from "@/components/shared/form/field-hint";
import { cn } from "@/lib/utils";

/**
 * A list setting of the Lanes & Planes parameter: one input per entry (rendered by [renderInput]), each
 * removable, plus an add button. Rows of the JSON value, not of a form array, so an emptied row is simply
 * posted empty and ignored by the backend.
 */
export function LanesAndPlanesRows<T>({
  label,
  hint,
  invalid,
  ariaLabel,
  rows,
  empty,
  onChange,
  renderInput,
}: {
  /** Without a label, the caller shows the header (see ListOrTextSetting). */
  label?: string;
  hint?: string;
  /** The backend reported an error about this setting: its label is marked. */
  invalid?: boolean;
  /** Names the inputs and removing buttons of the rows ("<ariaLabel> 2"). */
  ariaLabel: string;
  rows: T[];
  /** The value of a new row. */
  empty: T;
  onChange: (rows: T[]) => void;
  renderInput: (
    value: T,
    set: (value: T) => void,
    ariaLabel: string
  ) => ReactNode;
}) {
  const t = useTranslations("lanesAndPlanes.config");
  const set = (index: number, value: T) =>
    onChange(rows.map((row, i) => (i === index ? value : row)));
  return (
    <div className="grid gap-2">
      {label && (
        <div className="flex items-center gap-1">
          <span
            className={cn(
              "text-[11.5px] font-semibold uppercase tracking-wide text-muted-foreground",
              invalid && "text-destructive"
            )}
          >
            {label}
          </span>
          {hint && <FieldHint hint={hint} label={label} />}
        </div>
      )}
      {rows.map((row, index) => (
        <div
          // The rows have no identity of their own; the index is what the JSON array has.
          key={index}
          className="grid grid-cols-[1fr_auto] items-center gap-2"
        >
          {renderInput(
            row,
            (value) => set(index, value),
            `${ariaLabel} ${index + 1}`
          )}
          <Button
            type="button"
            variant="ghost"
            size="icon"
            aria-label={t("remove", { arg0: `${ariaLabel} ${index + 1}` })}
            onClick={() => onChange(rows.filter((_, i) => i !== index))}
          >
            <HugeiconsIcon icon={Delete02Icon} className="h-4 w-4" />
          </Button>
        </div>
      ))}
      <div>
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={() => onChange([...rows, empty])}
        >
          <HugeiconsIcon icon={PlusSignIcon} className="h-4 w-4" />
          {t("add")}
        </Button>
      </div>
    </div>
  );
}
