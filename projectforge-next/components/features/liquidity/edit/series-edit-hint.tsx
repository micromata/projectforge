"use client";

import { useTranslations } from "next-intl";
import { cn } from "@/lib/utils";

/**
 * The help text at the top of the focused series editor: editing the series changes only the future,
 * still-virtual occurrences — materialized (touched/paid) ones are frozen, independent entries and stay
 * unchanged (the plan's copy-on-write model). Mirrors the occurrence form's hint in [SeriesLink], so both
 * edit forms say up front what a save here does and does not touch.
 *
 * A custom field because it is descriptive text, not an input; placed first so it reads before the rule.
 */
export function SeriesEditHint({ className }: { className?: string }) {
  const t = useTranslations();
  return (
    <p className={cn("text-sm text-muted-foreground", className)}>
      {t("plugins.liquidityplanning.series.editHint")}
    </p>
  );
}
