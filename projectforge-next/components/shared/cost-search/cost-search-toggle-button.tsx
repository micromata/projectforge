"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Calculator01Icon } from "@hugeicons/core-free-icons";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Button } from "@/components/ui/button";
import { useCostSearchPanelStore } from "@/store/cost-search-panel-store";
import { useCostSearchAccess } from "./use-cost-search-access";

/** The top bar's button showing and hiding the floating cost search (CostSearchPanelHost). */
export function CostSearchToggleButton() {
  const t = useTranslations();
  const allowed = useCostSearchAccess();
  const open = useCostSearchPanelStore((s) => s.open);
  const toggle = useCostSearchPanelStore((s) => s.toggle);
  if (!allowed) return null;
  const label = t("fibu.kostSearch.panel.toggle");
  return (
    <HintTooltip text={label}>
      <Button
        type="button"
        variant="ghost"
        size="sm"
        className="shrink-0"
        aria-label={label}
        aria-pressed={open}
        data-testid="cost-search-toggle"
        onClick={toggle}
      >
        <HugeiconsIcon icon={Calculator01Icon} size={18} />
      </Button>
    </HintTooltip>
  );
}
