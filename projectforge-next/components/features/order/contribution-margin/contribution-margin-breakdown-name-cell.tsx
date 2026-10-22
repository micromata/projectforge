"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowRight01Icon } from "@hugeicons/core-free-icons";
import { cn } from "@/lib/utils";
import type {
  ContributionMarginBreakdownRow,
  ContributionMarginLevel,
} from "@/lib/rs/order";
import { useBreakdownRowLabel } from "./contribution-margin-breakdown-label";
import { depthPadding } from "./contribution-margin-breakdown-rows";
import { ContributionMarginResidualDetails } from "./contribution-margin-residual-details";

/**
 * The name of a row of the breakdown tree, indented by its depth, with the button expanding or collapsing
 * its children (not selecting the row) and, for the residual, what it consists of.
 */
export function ContributionMarginBreakdownNameCell({
  row,
  level,
  expanded,
  onToggle,
}: {
  row: ContributionMarginBreakdownRow;
  level: ContributionMarginLevel;
  expanded: boolean;
  onToggle: (id: string) => void;
}) {
  const tc = useTranslations();
  const rowLabel = useBreakdownRowLabel();
  const label = rowLabel(row);
  return (
    <div className={cn("flex items-start gap-1", depthPadding(row.depth))}>
      {row.hasChildren ? (
        <button
          type="button"
          className="mt-0.5 shrink-0 rounded text-muted-foreground hover:text-foreground"
          aria-expanded={expanded}
          aria-label={`${expanded ? tc("collapse") : tc("expand")}: ${label}`}
          onClick={(event) => {
            event.stopPropagation();
            onToggle(row.id);
          }}
        >
          <HugeiconsIcon
            icon={ArrowRight01Icon}
            strokeWidth={2}
            className={cn(
              "size-4 transition-transform",
              expanded && "rotate-90"
            )}
          />
        </button>
      ) : (
        <span className="size-4 shrink-0" />
      )}
      <div className="min-w-0 whitespace-normal">
        <div className={cn(row.type === "RESIDUAL" && "italic")}>
          {label}
          {row.type === "PROJECT" && row.kost && (
            <span className="ml-2 font-normal text-muted-foreground">
              {row.kost}
            </span>
          )}
        </div>
        {row.residual && (
          <ContributionMarginResidualDetails
            residual={row.residual}
            level={level}
          />
        )}
      </div>
    </div>
  );
}
