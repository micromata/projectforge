"use client";

import type { ReactNode } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Cancel01Icon, Tick02Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";

interface GanttValueCellProps {
  /** Whether the value differs from the structure element's — only then the two buttons show. */
  modified: boolean;
  /** The structure element's value as text, for the reject tooltip. */
  taskValue: string;
  /** Whether the user may write to the structure element (`TaskValues.updateAccess`). */
  canSave: boolean;
  saving?: boolean;
  onReject: () => void;
  onSave: () => void;
  children: ReactNode;
}

/**
 * One editable value of a Gantt row, with Wicket's reject/save pair beside it once the value deviates from
 * the structure element: reject takes the element's value over, save writes this one to the element
 * (RejectSaveLinksFragment).
 */
export function GanttValueCell({
  modified,
  taskValue,
  canSave,
  saving,
  onReject,
  onSave,
  children,
}: GanttValueCellProps) {
  const t = useTranslations();
  return (
    <div className="flex min-w-0 items-center gap-0.5">
      <div className="min-w-0 flex-1">{children}</div>
      {modified && (
        <>
          <HintTooltip
            text={t("gantt.tooltip.rejectValue", { arg0: taskValue })}
            plain
          >
            <Button
              type="button"
              variant="ghost"
              size="icon-xs"
              className="text-destructive"
              aria-label={t("gantt.tooltip.rejectValue", { arg0: taskValue })}
              onClick={onReject}
            >
              <HugeiconsIcon icon={Cancel01Icon} size={14} />
            </Button>
          </HintTooltip>
          {canSave && (
            <HintTooltip text={t("gantt.tooltip.saveTaskValue")}>
              <Button
                type="button"
                variant="ghost"
                size="icon-xs"
                className="text-brand-green-dark"
                disabled={saving}
                aria-label={t("gantt.tooltip.saveTaskValue")}
                onClick={onSave}
              >
                <HugeiconsIcon icon={Tick02Icon} size={14} />
              </Button>
            </HintTooltip>
          )}
        </>
      )}
    </div>
  );
}
