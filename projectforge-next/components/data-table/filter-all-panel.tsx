"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import type { FilterElement } from "@/lib/rs/types";
import { FilterFieldGrid } from "./filter-field-grid";
import type { FilterValues } from "./filter-value";

interface FilterAllPanelProps {
  elements: FilterElement[];
  /** The applied filters; the panel edits a copy of them. */
  initial: FilterValues;
  onApply: (values: FilterValues) => void;
  /** Leaves the panel without applying — back to the field list. */
  onCancel: () => void;
}

/**
 * Every filter field of the list at once — the overview the pill row can't give.
 *
 * Edits a draft and only applies on demand: each applied change is a new query key, so filtering
 * per keystroke would refetch the list while the panel is still open. Mount this only while the
 * panel is visible, so every draft starts from the applied filters.
 */
export function FilterAllPanel({
  elements,
  initial,
  onApply,
  onCancel,
}: FilterAllPanelProps) {
  const t = useTranslations("filter");
  const tAction = useTranslations();
  const [draft, setDraft] = useState(initial);

  return (
    <div className="space-y-3">
      <p className="border-b pb-2 text-sm font-semibold">{t("allFilters")}</p>
      <FilterFieldGrid
        elements={elements}
        values={draft}
        onChange={setDraft}
        className="max-h-[60vh] overflow-y-auto pr-1"
      />
      {/* "Cancel" is also the way back to the field list — the panel is one view of the picker. */}
      <div className="flex items-center gap-2 border-t pt-2">
        <Button
          variant="ghost"
          size="sm"
          className="mr-auto"
          onClick={() => setDraft({})}
        >
          {t("reset")}
        </Button>
        <Button variant="outline" size="sm" onClick={onCancel}>
          {tAction("cancel")}
        </Button>
        <Button size="sm" onClick={() => onApply(draft)}>
          {tAction("apply")}
        </Button>
      </div>
    </div>
  );
}
