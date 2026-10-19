"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { PictureInPictureOnIcon } from "@hugeicons/core-free-icons";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { Button } from "@/components/ui/button";
import { CostSearchConverter } from "./cost-search-converter";
import { useDockCostSearch } from "./use-dock-cost-search";

/** The cost search as a page of its own, the menu entry's target. */
export function CostSearchPage() {
  const t = useTranslations();
  const dock = useDockCostSearch();
  return (
    <PageShell>
      <PageTitleRow
        category={t("menu.fibu._")}
        title={t("menu.fibu.kostSearch")}
      >
        <HintTooltip text={t("fibu.kostSearch.dockTooltip")}>
          <Button type="button" variant="outline" size="sm" onClick={dock}>
            <HugeiconsIcon icon={PictureInPictureOnIcon} size={16} />
            {t("fibu.kostSearch.dock")}
          </Button>
        </HintTooltip>
      </PageTitleRow>
      <div className="flex max-w-xl flex-col gap-3 px-4 pb-4">
        <p className="text-xs text-muted-foreground">
          {t("fibu.kostSearch.hint")}
        </p>
        <CostSearchConverter />
      </div>
    </PageShell>
  );
}
