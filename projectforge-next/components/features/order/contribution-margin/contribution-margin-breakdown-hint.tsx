"use client";

import { useTranslations } from "next-intl";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency } from "@/lib/format";

/**
 * That the values of orders and tasks are calculated (invoice positions, time sheets × hourly rate) and the
 * rest stays with the project, and a warning if no hourly rate is configured (no time sheet costs at all).
 */
export function ContributionMarginBreakdownHint({
  hourlyRate,
}: {
  hourlyRate: number | null;
}) {
  const t = useTranslations("fibu.auftrag.contributionMargin");
  const ctx = useFormatContext();
  return (
    <Alert>
      <AlertDescription className="space-y-1">
        <p>
          {t("breakdown.calculated", {
            arg0: hourlyRate != null ? formatCurrency(hourlyRate, ctx) : "–",
          })}
        </p>
        {hourlyRate == null && <p>{t("breakdown.hourlyRateMissing")}</p>}
      </AlertDescription>
    </Alert>
  );
}
