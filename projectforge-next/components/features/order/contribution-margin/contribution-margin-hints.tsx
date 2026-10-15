"use client";

import { useTranslations } from "next-intl";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { useFormatContext } from "@/hooks/use-format";
import { formatDate } from "@/lib/format";
import type { ContributionMarginData } from "@/lib/rs/order";

/**
 * What the figures are made of, where it isn't the accounting records alone: the preliminary months after
 * the last import (and a missing hourly rate for their time sheets), and the orders without a project.
 * Nothing if all months are booked and every order has a project.
 */
export function ContributionMarginHints({
  data,
}: {
  data: ContributionMarginData;
}) {
  const t = useTranslations("fibu.auftrag.contributionMargin");
  const ctx = useFormatContext();
  const hasPreliminary = data.preliminary.some(Boolean);
  if (!hasPreliminary && data.ordersWithoutProject === 0) {
    return null;
  }
  return (
    <Alert>
      <AlertDescription className="space-y-1">
        {hasPreliminary && (
          <p>
            {data.bookingImportEnd
              ? t("bookingImportEnd", {
                  arg0: formatDate(data.bookingImportEnd, ctx),
                })
              : t("noImport")}
          </p>
        )}
        {data.hourlyRateMissing && <p>{t("hourlyRateMissing")}</p>}
        {data.ordersWithoutProject > 0 && (
          <p>
            {t("ordersWithoutProject", { arg0: data.ordersWithoutProject })}
          </p>
        )}
      </AlertDescription>
    </Alert>
  );
}
