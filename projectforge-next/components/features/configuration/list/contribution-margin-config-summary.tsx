"use client";

import { useTranslations } from "next-intl";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency } from "@/lib/format";
import { parseContributionMarginConfig } from "../contribution-margin-config";

/** The settings of the contribution margin in one line, for the value column of the list. */
export function ContributionMarginConfigSummary({
  json,
}: {
  json: string | null | undefined;
}) {
  const t = useTranslations("fibu.auftrag.contributionMargin.config");
  const ctx = useFormatContext();
  const { config, invalid } = parseContributionMarginConfig(json);
  if (invalid) {
    return <span className="text-warning">{t("invalidJson")}</span>;
  }
  const assignments = config.kost2Assignments.filter(
    (row) => row.kost2 || row.project
  ).length;
  return (
    <span className="tabular-nums">
      {t("summary", {
        arg0: config.targetPercentage,
        arg1: config.redThreshold,
        arg2: assignments,
      })}
      {config.hourlyRate != null &&
        ` · ${t("hourlyRate._")} ${formatCurrency(config.hourlyRate, ctx)}`}
    </span>
  );
}
