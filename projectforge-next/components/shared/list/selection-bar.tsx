"use client";

import type { ReactNode } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { HelpCircleIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";

/**
 * The bar above the table while a list is in selection mode: how many are picked, and what can be
 * done with them.
 *
 * Visible for the whole mode rather than only once something is ticked — it is what tells the user
 * which mode the list is in, and it is where they leave it again. The mass update button comes in as a
 * slot: which page the picked entries go to is the entity's business (see MassUpdateDef), while
 * ticking, clearing and leaving are the mode's.
 */
export function SelectionBar({
  count,
  onSelectAll,
  onClear,
  onLeave,
  actions,
}: {
  count: number;
  onSelectAll: () => void;
  onClear: () => void;
  onLeave: () => void;
  /** The mass update button, rendered right of the counts. */
  actions?: ReactNode;
}) {
  const t = useTranslations();
  return (
    <div className="flex flex-wrap items-center gap-2 border-b bg-primary/5 px-4 py-1.5 text-xs">
      {/* The count is the whole heading: what mode the list is in is already said by the pressed
          toggle above and by the checkbox column, so naming it a third time here only repeats it. */}
      <span className="font-semibold">
        {t("massUpdate.entriesFound", { arg0: count })}
      </span>
      {/* How to pick rows, as markdown from the bundle — the gestures it lists are the ones
          `use-row-selection` implements, plus the shortcut that ticks the whole list. Behind an icon,
          because a reader needs it once. The key still names ag-grid, the legacy grid the text was
          written for; duplicating it under a nicer key would mean two translations to keep in step.
          `._`: the shortcut key has a child (.title), so it is nested under `_` in the catalog. */}
      <HintTooltip
        title={t("multiselection.aggrid.selection.info.title")}
        text={`${t("multiselection.aggrid.selection.info.message")}\n\n* **${t(
          "tooltip.shortcut.selectAll.title"
        )}**: ${t("tooltip.shortcut.selectAll._")}`}
      >
        <Button
          type="button"
          variant="ghost"
          size="icon"
          className="size-6"
          aria-label={t("multiselection.aggrid.selection.info.title")}
        >
          <HugeiconsIcon icon={HelpCircleIcon} size={14} aria-hidden />
        </Button>
      </HintTooltip>
      <div className="flex-1" />
      <Button
        type="button"
        variant="ghost"
        size="sm"
        className="h-6 px-2"
        onClick={onSelectAll}
      >
        {t("selectAll")}
      </Button>
      <Button
        type="button"
        variant="ghost"
        size="sm"
        className="h-6 px-2"
        disabled={count === 0}
        onClick={onClear}
      >
        {t("deselectAll")}
      </Button>
      {actions}
      {/* The way out, last: leaving drops the selection, so it sits after everything that uses it. */}
      <Button
        type="button"
        variant="outline"
        size="sm"
        className="h-6 px-2"
        onClick={onLeave}
      >
        {t("cancel")}
      </Button>
    </div>
  );
}
