"use client";

import { useState } from "react";
import { HugeiconsIcon } from "@hugeicons/react";
import { FilterIcon } from "@hugeicons/core-free-icons";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import type { FilterElement } from "@/lib/rs/types";
import { FilterFieldGrid } from "./filter-field-grid";
import type { FilterValues } from "./filter-value";

interface FilterAllDialogProps {
  elements: FilterElement[];
  /** The applied filters; the dialog edits a copy of them. */
  values: FilterValues;
  onApply: (values: FilterValues) => void;
  className?: string;
}

/**
 * Every filter field of the list at once — the overview the pill row can't give.
 *
 * Edits a draft and only applies on demand: each applied change is a new query key, so
 * filtering per keystroke would refetch the list while the dialog is still open.
 */
export function FilterAllDialog({
  elements,
  values,
  onApply,
  className,
}: FilterAllDialogProps) {
  const t = useTranslations("filter");
  const [open, setOpen] = useState(false);
  const activeCount = Object.keys(values).length;

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button
          variant="outline"
          size="sm"
          className={className}
          aria-label={`${t("allFilters")} – ${t("activeCount", { arg0: activeCount })}`}
        >
          <HugeiconsIcon icon={FilterIcon} size={13} />
          {t("allFilters")}
          {activeCount > 0 && (
            <span
              aria-hidden
              className="rounded-full bg-primary px-1.5 text-[10px] font-bold text-primary-foreground"
            >
              {activeCount}
            </span>
          )}
        </Button>
      </DialogTrigger>
      {/* Mounted only while open, so every draft starts from the applied filters. */}
      {open && (
        <FilterAllDialogBody
          elements={elements}
          initial={values}
          onApply={(next) => {
            onApply(next);
            setOpen(false);
          }}
          onCancel={() => setOpen(false)}
        />
      )}
    </Dialog>
  );
}

function FilterAllDialogBody({
  elements,
  initial,
  onApply,
  onCancel,
}: {
  elements: FilterElement[];
  initial: FilterValues;
  onApply: (values: FilterValues) => void;
  onCancel: () => void;
}) {
  const t = useTranslations("filter");
  const tAction = useTranslations();
  const [draft, setDraft] = useState(initial);

  return (
    // DialogContent is sized for a small confirm dialog, so width, rows and scrolling
    // all have to be overridden here — components/ui/ is off limits.
    <DialogContent
      aria-describedby={undefined}
      className="grid-rows-[auto_1fr_auto] gap-3 sm:max-w-none max-h-[85vh] w-[min(1100px,calc(100vw-2rem))] max-w-none overflow-hidden"
    >
      <DialogHeader>
        <DialogTitle>{t("allFilters")}</DialogTitle>
      </DialogHeader>
      <FilterFieldGrid
        elements={elements}
        values={draft}
        onChange={setDraft}
        className="overflow-y-auto pr-1"
      />
      <DialogFooter>
        <Button
          variant="ghost"
          size="sm"
          className="sm:mr-auto"
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
      </DialogFooter>
    </DialogContent>
  );
}
