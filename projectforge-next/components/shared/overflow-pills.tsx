"use client";

import { useTranslations } from "next-intl";
import { Badge } from "@/components/ui/badge";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { cn } from "@/lib/utils";

/** How many pills are shown before the rest collapses into "+k". */
const DEFAULT_MAX = 3;

/**
 * A selection as pills that stays one line: the first [max] labels, then a "+k" pill whose tooltip
 * lists every selected entry — a selection of forty customers would otherwise push the form apart.
 */
export function OverflowPills({
  labels,
  max = DEFAULT_MAX,
  placeholder,
  className,
}: {
  labels: string[];
  max?: number;
  /** Shown while nothing is selected. */
  placeholder?: string;
  className?: string;
}) {
  const t = useTranslations();
  if (labels.length === 0) {
    return (
      <span className={cn("truncate text-muted-foreground", className)}>
        {placeholder}
      </span>
    );
  }
  const shown = labels.slice(0, max);
  const rest = labels.length - shown.length;
  return (
    <span className={cn("flex min-w-0 items-center gap-1", className)}>
      {shown.map((label) => (
        <Badge key={label} variant="secondary" className="max-w-40 truncate">
          {label}
        </Badge>
      ))}
      {rest > 0 && (
        <HintTooltip text={labels.join("\n")} plain>
          <Badge
            variant="outline"
            className="shrink-0"
            aria-label={t("select.checklist.andMore", { arg0: rest })}
          >
            +{rest}
          </Badge>
        </HintTooltip>
      )}
    </span>
  );
}
