"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowRight01Icon, Cancel01Icon } from "@hugeicons/core-free-icons";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { cn } from "@/lib/utils";
import type { FilterElement, MagicFilterEntryValue } from "@/lib/rs/types";
import { FilterPanelField } from "./filter-panel-field";

export type FilterValues = Record<string, MagicFilterEntryValue>;

interface FilterPanelProps {
  elements: FilterElement[];
  values: FilterValues;
  onChange: (values: FilterValues) => void;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  className?: string;
}

/**
 * Collapsible side panel with the filter fields the backend offers for this list.
 *
 * Unlike the column filters in the header, these are applied server-side through
 * MagicFilter.entries — the field set comes from the layout, so it differs per
 * entity and can't be hard-coded here.
 */
export function FilterPanel({
  elements,
  values,
  onChange,
  open,
  onOpenChange,
  className,
}: FilterPanelProps) {
  const t = useTranslations("filter");
  const activeCount = Object.keys(values).length;

  if (!open) {
    return (
      <div className="flex shrink-0 items-start border-l p-1">
        <Button
          variant="ghost"
          size="sm"
          onClick={() => onOpenChange(true)}
          aria-label={t("show")}
          title={t("show")}
          className="gap-1"
        >
          <HugeiconsIcon icon={ArrowRight01Icon} size={14} className="rotate-180" />
          {activeCount > 0 && (
            <span className="rounded-full bg-primary px-1.5 text-[10px] font-bold text-primary-foreground">
              {activeCount}
            </span>
          )}
        </Button>
      </div>
    );
  }

  return (
    <aside
      className={cn(
        "flex w-72 shrink-0 flex-col overflow-hidden border-l bg-background",
        className
      )}
    >
      <div className="flex items-center gap-2 border-b px-3 py-2">
        <h2 className="flex-1 text-sm font-semibold">{t("title")}</h2>
        {activeCount > 0 && (
          <Button
            variant="ghost"
            size="sm"
            className="h-6 text-xs"
            onClick={() => onChange({})}
          >
            {t("reset")}
          </Button>
        )}
        <Button
          variant="ghost"
          size="icon"
          className="h-6 w-6"
          onClick={() => onOpenChange(false)}
          aria-label={t("hide")}
          title={t("hide")}
        >
          <HugeiconsIcon icon={Cancel01Icon} size={13} />
        </Button>
      </div>

      <div className="flex-1 space-y-3 overflow-y-auto p-3">
        {elements.length === 0 ? (
          <p className="text-xs text-muted-foreground">{t("noFields")}</p>
        ) : (
          elements.map((element, index) => (
            <div key={element.id}>
              {index > 0 && <Separator className="mb-3" />}
              <FilterPanelField
                element={element}
                value={values[element.id]}
                onChange={(value) => {
                  const next = { ...values };
                  if (value === undefined) delete next[element.id];
                  else next[element.id] = value;
                  onChange(next);
                }}
              />
            </div>
          ))
        )}
      </div>
    </aside>
  );
}
