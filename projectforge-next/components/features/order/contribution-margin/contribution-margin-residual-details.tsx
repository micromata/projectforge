"use client";

import { useTranslations } from "next-intl";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency } from "@/lib/format";
import type {
  ContributionMarginLevel,
  ContributionMarginResidual,
} from "@/lib/rs/order";

/**
 * What the residual of a project consists of in the period (see ContributionMarginResidual), each part only
 * if there is any: invoices and time sheets without an order (or task), and the difference to accounting.
 */
export function ContributionMarginResidualDetails({
  residual,
  level,
}: {
  residual: ContributionMarginResidual;
  level: ContributionMarginLevel;
}) {
  const t = useTranslations("fibu.auftrag.contributionMargin");
  const ctx = useFormatContext();
  const invoices = {
    arg0: formatCurrency(residual.invoicesWithoutAssignment, ctx),
  };
  return (
    <div className="flex flex-col text-xs not-italic text-muted-foreground">
      {residual.invoicesWithoutAssignment !== 0 && (
        <span>
          {level === "ORDER"
            ? t("breakdown.residual.invoices", invoices)
            : t("breakdown.residual.invoicesTask", invoices)}
        </span>
      )}
      {residual.timesheetsWithoutAssignment !== 0 && (
        <span>
          {t("breakdown.residual.timesheets", {
            arg0: formatCurrency(residual.timesheetsWithoutAssignment, ctx),
          })}
        </span>
      )}
      {residual.accountingDifference !== 0 && (
        <HintTooltip
          text={t("breakdown.residual.accounting.tooltip")}
          openOnTap
        >
          <span className="w-fit underline decoration-dotted">
            {t("breakdown.residual.accounting._", {
              arg0: formatCurrency(residual.accountingDifference, ctx),
            })}
          </span>
        </HintTooltip>
      )}
    </div>
  );
}
