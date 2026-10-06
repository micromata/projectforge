"use client";

import type { ReactNode } from "react";
import { HugeiconsIcon } from "@hugeicons/react";
import { DashboardSquare01Icon, ViewIcon } from "@hugeicons/core-free-icons";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { HintTooltip } from "@/components/shared/hint-tooltip";

/**
 * The controls over a ChartDashboard: "Arrange" outside edit mode; in edit mode the hidden tiles to show
 * again, "Reset layout" and "Done".
 */
export function DashboardToolbar({
  editing,
  onEditingChange,
  hidden,
  onShow,
  canReset,
  onReset,
}: {
  editing: boolean;
  onEditingChange: (editing: boolean) => void;
  hidden: { id: string; title: ReactNode }[];
  onShow: (id: string) => void;
  canReset: boolean;
  onReset: () => void;
}) {
  const t = useTranslations("dashboard");
  if (!editing) {
    return (
      <div className="flex justify-end">
        <HintTooltip text={t("arrange.tooltip")}>
          <Button
            variant="ghost"
            size="sm"
            className="gap-1.5 text-muted-foreground"
            onClick={() => onEditingChange(true)}
          >
            <HugeiconsIcon icon={DashboardSquare01Icon} size={14} />
            {t("arrange._")}
          </Button>
        </HintTooltip>
      </div>
    );
  }
  return (
    <div className="flex flex-wrap items-center justify-end gap-2">
      {hidden.length > 0 && (
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="outline" size="sm" className="gap-1.5">
              <HugeiconsIcon icon={ViewIcon} size={14} />
              {t("hiddenTiles", { arg0: hidden.length })}
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            {hidden.map((tile) => (
              <DropdownMenuItem key={tile.id} onSelect={() => onShow(tile.id)}>
                {tile.title}
              </DropdownMenuItem>
            ))}
          </DropdownMenuContent>
        </DropdownMenu>
      )}
      <Button
        variant="outline"
        size="sm"
        disabled={!canReset}
        onClick={onReset}
      >
        {t("reset")}
      </Button>
      <Button size="sm" onClick={() => onEditingChange(false)}>
        {t("done")}
      </Button>
    </div>
  );
}
