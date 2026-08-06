"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { cn } from "@/lib/utils";
import type { FilterElement } from "@/lib/rs/types";
import { FilterFieldPicker } from "./filter-field-picker";
import { FilterPill } from "./filter-pill";
import { withFilterValue, type FilterValues } from "./filter-value";

interface FilterPillsProps {
  /** Field definitions, for labels and for resolving LIST values to their text. */
  elements: FilterElement[];
  values: FilterValues;
  onChange: (values: FilterValues) => void;
  /** Sits next to the "add filter" chip — meant for the "all filters" dialog trigger. */
  trailing?: React.ReactNode;
  className?: string;
}

/**
 * The list's filters as pills: each one opens its own input, and the "add filter" chip
 * picks another field. This is the primary filter surface — the "all filters" dialog only
 * adds an overview of everything the backend offers.
 */
export function FilterPills({
  elements,
  values,
  onChange,
  trailing,
  className,
}: FilterPillsProps) {
  const t = useTranslations("filter");
  const [openId, setOpenId] = useState<string | null>(null);
  // A field picked from the chip: shown as an empty pill until it is saved or dropped.
  const [pendingId, setPendingId] = useState<string | null>(null);
  const activeCount = Object.keys(values).length;

  // Derived, and in backend order, so pills don't jump around as values come and go.
  const shown = elements.filter(
    (element) =>
      element.defaultFilter || element.id in values || element.id === pendingId
  );

  function close() {
    setOpenId(null);
    setPendingId(null);
  }

  return (
    <div className={cn("flex flex-wrap items-center gap-1.5", className)}>
      {shown.map((element) => (
        <FilterPill
          key={element.id}
          element={element}
          value={values[element.id]}
          open={openId === element.id}
          onOpenChange={(open) => (open ? setOpenId(element.id) : close())}
          removable={!element.defaultFilter}
          onSave={(value) => {
            onChange(withFilterValue(values, element.id, value));
            close();
          }}
          onDelete={() => {
            onChange(withFilterValue(values, element.id, undefined));
            close();
          }}
        />
      ))}
      <FilterFieldPicker
        elements={elements}
        activeIds={shown.map((element) => element.id)}
        onSelect={(id) => {
          // Keyed by id, so a pending pill mounts with its popover already open.
          if (!(id in values)) setPendingId(id);
          setOpenId(id);
        }}
      />
      {activeCount > 0 && (
        <button
          type="button"
          onClick={() => {
            close();
            onChange({});
          }}
          className="cursor-pointer px-1 text-xs font-medium text-muted-foreground hover:text-foreground"
        >
          {t("clearAll")}
        </button>
      )}
      {trailing}
    </div>
  );
}
