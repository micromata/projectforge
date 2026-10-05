"use client";

import { cn } from "@/lib/utils";
import { useStatisticsLabels } from "../statistics/use-statistics-labels";

/**
 * Preliminary values (unbooked invoices, time sheets × hourly rate, see ContributionMarginService) are
 * written in this colour in the data tables, as the DB Excel writes them in blue.
 */
export const PRELIMINARY_CLASS = "text-status-info";

/** The legend under a table naming what its coloured values are. */
export function ContributionMarginPreliminaryLegend() {
  const t = useStatisticsLabels();
  return (
    <p className={cn("text-xs", PRELIMINARY_CLASS)}>{t.preliminaryLegend}</p>
  );
}
