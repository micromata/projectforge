"use client";

import { useState } from "react";
import { HugeiconsIcon } from "@hugeicons/react";
import { Cancel01Icon } from "@hugeicons/core-free-icons";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { cn } from "@/lib/utils";
import type { FilterElement, MagicFilterEntryValue } from "@/lib/rs/types";
import { FilterField } from "./filter-field";
import { describeFilterValue, isEmptyFilterValue } from "./filter-value";

interface FilterPillProps {
  element: FilterElement;
  value: MagicFilterEntryValue | undefined;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** Default filters stay on the row, so they only offer emptying, not removing. */
  removable: boolean;
  onSave: (value: MagicFilterEntryValue | undefined) => void;
  onDelete: () => void;
}

/**
 * One filter as a pill whose popover holds its input — the primary way to filter a
 * list, as in the legacy webapp. Editing happens on a draft so the list is only
 * refetched once, on save.
 */
export function FilterPill({
  element,
  value,
  open,
  onOpenChange,
  removable,
  onSave,
  onDelete,
}: FilterPillProps) {
  const t = useTranslations("filter");
  const tAction = useTranslations();
  const [draft, setDraft] = useState(value);
  const label = element.label ?? element.id;
  const text = describeFilterValue(value, element);
  const active = !isEmptyFilterValue(value);

  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full border text-xs font-medium",
        active
          ? "border-primary/30 bg-primary/10 text-primary"
          : "border-dashed border-muted-foreground/40 text-muted-foreground"
      )}
    >
      <Popover
        open={open}
        onOpenChange={(next) => {
          // Re-seed on open so an abandoned edit doesn't come back.
          if (next) setDraft(value);
          onOpenChange(next);
        }}
      >
        <PopoverTrigger asChild>
          <button
            type="button"
            title={element.tooltip}
            aria-label={t("editEntry", { arg0: label })}
            className="max-w-64 cursor-pointer truncate rounded-full px-2.5 py-0.5"
          >
            {label}
            {text && `: ${text}`}
          </button>
        </PopoverTrigger>
        <PopoverContent align="start" className="w-72 space-y-2 p-3">
          <FilterField
            element={element}
            value={draft}
            onChange={setDraft}
            autoFocus
            onSubmit={() => save()}
          />
          <div className="flex justify-end gap-1">
            <Button
              variant="ghost"
              size="sm"
              className="h-7 text-xs"
              onClick={onDelete}
            >
              {tAction("delete")}
            </Button>
            <Button size="sm" className="h-7 text-xs" onClick={() => save()}>
              {tAction("save")}
            </Button>
          </div>
        </PopoverContent>
      </Popover>
      {removable && (
        <button
          type="button"
          onClick={onDelete}
          aria-label={t("removeEntry", { arg0: label })}
          className="mr-1.5 flex size-4 shrink-0 cursor-pointer items-center justify-center rounded-full hover:bg-primary/20"
        >
          <HugeiconsIcon icon={Cancel01Icon} size={10} />
        </button>
      )}
    </span>
  );

  /** Saving an emptied field removes it, as in the legacy MagicInput.isEmpty check. */
  function save() {
    onSave(isEmptyFilterValue(draft) ? undefined : draft);
  }
}
