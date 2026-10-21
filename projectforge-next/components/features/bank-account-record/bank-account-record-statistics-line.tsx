"use client";

import { useTranslations } from "next-intl";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency } from "@/lib/format";
import { cn } from "@/lib/utils";
import type { BankAccountRecordStatistics } from "./types";

/**
 * The income, outgo and cash flow of the whole result set above the table, as the React list showed them
 * (`BankAccountRecordStatistics`): what is negative reads red.
 */
export function BankAccountRecordStatisticsLine({
  statistics,
  isFetching,
}: {
  statistics: BankAccountRecordStatistics | undefined;
  isFetching: boolean;
}) {
  const t = useTranslations();
  const ctx = useFormatContext();
  if (!statistics) return null;
  const entries = [
    {
      key: "income",
      labelKey: "plugins.banking.account.record.income",
      value: statistics.income,
    },
    {
      key: "outgo",
      labelKey: "plugins.banking.account.record.outgo",
      value: statistics.outgo,
    },
    {
      key: "cashflow",
      labelKey: "plugins.banking.account.record.cashflow",
      value: statistics.cashflow,
    },
  ];
  return (
    <div
      className={cn(
        "flex flex-wrap gap-x-6 gap-y-1 px-1 text-sm",
        isFetching && "opacity-60"
      )}
    >
      {entries.map((entry) => (
        <span key={entry.key}>
          <span className="text-muted-foreground">{t(entry.labelKey)}: </span>
          <span
            className={cn(
              "font-semibold tabular-nums",
              entry.value < 0 && "text-destructive"
            )}
          >
            {formatCurrency(entry.value, ctx)}
          </span>
        </span>
      ))}
    </div>
  );
}
