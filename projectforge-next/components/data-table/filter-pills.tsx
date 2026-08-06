"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import { Cancel01Icon } from "@hugeicons/core-free-icons";
import { useTranslations } from "next-intl";
import { cn } from "@/lib/utils";
import type { FilterElement, MagicFilterEntryValue } from "@/lib/rs/types";
import type { FilterValues } from "./filter-panel";

interface FilterPillsProps {
  /** Field definitions, for labels and for resolving LIST values to their text. */
  elements: FilterElement[];
  values: FilterValues;
  onChange: (values: FilterValues) => void;
  className?: string;
}

/**
 * The active filters as removable pills, so what narrows the list stays visible
 * even with the filter panel collapsed.
 */
export function FilterPills({
  elements,
  values,
  onChange,
  className,
}: FilterPillsProps) {
  const t = useTranslations("filter");
  const active = Object.entries(values);

  if (active.length === 0) return null;

  function remove(field: string) {
    const next = { ...values };
    delete next[field];
    onChange(next);
  }

  return (
    <div className={cn("flex flex-wrap items-center gap-1.5", className)}>
      {active.map(([field, value]) => {
        const element = elements.find((e) => e.id === field);
        const label = element?.label ?? field;
        const text = describeValue(value, element);

        return (
          <span
            key={field}
            className="inline-flex items-center gap-1 rounded-full border border-primary/30 bg-primary/10 px-2.5 py-0.5 text-xs font-medium text-primary"
          >
            <span className="max-w-56 truncate">
              {label}
              {text && `: ${text}`}
            </span>
            <button
              type="button"
              onClick={() => remove(field)}
              aria-label={t("removeEntry", { arg0: label })}
              className="flex size-4 shrink-0 items-center justify-center rounded-full hover:bg-primary/20"
            >
              <HugeiconsIcon icon={Cancel01Icon} size={10} />
            </button>
          </span>
        );
      })}
      <button
        type="button"
        onClick={() => onChange({})}
        className="px-1 text-xs font-medium text-muted-foreground hover:text-foreground"
      >
        {t("clearAll")}
      </button>
    </div>
  );
}

/**
 * Renders a filter value the way it was entered: LIST ids resolve to their
 * display names, ranges read as "from – to", and the wildcards a STRING filter
 * needs for its LIKE query are stripped again.
 */
function describeValue(
  value: MagicFilterEntryValue,
  element: FilterElement | undefined
): string {
  if (value.values?.length) {
    return value.values
      .map((id) => element?.values?.find((v) => v.id === id)?.displayName ?? id)
      .join(", ");
  }
  if (value.from || value.to) {
    return [value.from, value.to].filter(Boolean).join(" – ");
  }
  if (value.displayName) return value.displayName;
  if (value.value == null) return "";
  // BOOLEAN filters carry "true"; the label alone already says what is meant.
  if (element?.filterType === "BOOLEAN") return "";
  return value.value.replace(/^\*(.*)\*$/, "$1");
}
