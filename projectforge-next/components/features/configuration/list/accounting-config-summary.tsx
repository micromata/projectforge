"use client";

import { useTranslations } from "next-intl";
import { parseAccountingConfig } from "../accounting-config";

/** The accounting settings in one line, for the value column of the list. */
export function AccountingConfigSummary({ json }: { json: string | null | undefined }) {
  const t = useTranslations();
  const { config, invalid } = parseAccountingConfig(json);
  if (invalid) {
    return (
      <span className="text-warning">
        {t("administration.configuration.accounting.invalidJson")}
      </span>
    );
  }
  const firstProjectNumbers = Object.entries(config.firstProjectNumbers)
    .map(([prefix, number]) => `${prefix}: ${number}`)
    .join(", ");
  return (
    <span className="tabular-nums">
      {t("administration.configuration.accounting.summary", {
        arg0: config.debitorsAccountNumberRanges.trim() || "–",
        arg1: config.creditorsAccountNumberRanges.trim() || "–",
        arg2: config.rows.length,
      })}
      {firstProjectNumbers &&
        `, ${t("administration.configuration.accounting.summaryFirstProjectNumbers", { arg0: firstProjectNumbers })}`}
    </span>
  );
}
