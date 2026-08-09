"use client";

import { useState } from "react";
import { HugeiconsIcon } from "@hugeicons/react";
import { PlusSignIcon } from "@hugeicons/core-free-icons";
import { useTranslations } from "next-intl";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { cn } from "@/lib/utils";
import type { FilterElement } from "@/lib/rs/types";
import { FilterAllPanel } from "./filter-all-panel";
import {
  FilterFieldList,
  type FilterFieldPickerEntry,
} from "./filter-field-list";
import type { FilterValues } from "./filter-value";

export type { FilterFieldPickerEntry };

interface FilterFieldPickerProps {
  entries: FilterFieldPickerEntry[];
  /** Fields already on the pill row; they get a checkmark. */
  activeIds: string[];
  onSelect: (id: string) => void;
  /** All field definitions, for the "all filters" panel. */
  elements: FilterElement[];
  /** The applied filters — the panel starts its draft from them. */
  values: FilterValues;
  onApply: (values: FilterValues) => void;
}

/**
 * The one way into the list's filters: pick a single field, or open every field at once.
 *
 * Both live behind this chip so the toolbar keeps a single entry point; the count of applied
 * filters rides on the chip because the panel that used to carry it is now inside.
 */
export function FilterFieldPicker({
  entries,
  activeIds,
  onSelect,
  elements,
  values,
  onApply,
}: FilterFieldPickerProps) {
  const t = useTranslations("filter");
  const [open, setOpen] = useState(false);
  const [showAll, setShowAll] = useState(false);
  const activeCount = Object.keys(values).length;

  function close() {
    setOpen(false);
    setShowAll(false);
  }

  return (
    <Popover
      open={open}
      onOpenChange={(next) => (next ? setOpen(true) : close())}
    >
      <PopoverTrigger asChild>
        {/* Icon-only: the pill row is where the eye goes, and a "+" chip reads as "add" there. */}
        <button
          type="button"
          title={t("addField")}
          aria-label={
            activeCount > 0
              ? `${t("addField")} – ${t("activeCount", { arg0: activeCount })}`
              : t("addField")
          }
          className="inline-flex cursor-pointer items-center gap-1 rounded-full border border-dashed border-muted-foreground/40 px-2 py-1 text-muted-foreground hover:border-primary/40 hover:text-primary"
        >
          <HugeiconsIcon icon={PlusSignIcon} size={13} />
          {activeCount > 0 && (
            <span
              aria-hidden
              className="rounded-full bg-primary px-1.5 text-[10px] font-bold text-primary-foreground"
            >
              {activeCount}
            </span>
          )}
        </button>
      </PopoverTrigger>
      <PopoverContent
        align="start"
        className={cn(
          showAll ? "w-[min(1100px,calc(100vw-2rem))] p-3" : "w-72 p-0"
        )}
      >
        {showAll ? (
          // Mounted only while shown, so every draft starts from the applied filters.
          <FilterAllPanel
            elements={elements}
            initial={values}
            onApply={(next) => {
              onApply(next);
              close();
            }}
            onCancel={() => setShowAll(false)}
          />
        ) : (
          <FilterFieldList
            entries={entries}
            activeIds={activeIds}
            onSelect={(id) => {
              close();
              onSelect(id);
            }}
            onShowAll={() => setShowAll(true)}
          />
        )}
      </PopoverContent>
    </Popover>
  );
}
