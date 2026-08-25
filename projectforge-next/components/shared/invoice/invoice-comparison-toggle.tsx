"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowDown01Icon, ArrowRight01Icon } from "@hugeicons/core-free-icons";
import { cn } from "@/lib/utils";

/**
 * The one control of the invoice statistics: a caret that expands the same figures a year earlier and
 * collapses them again. It sits inline — at the start of the compact line, or in the corner of the
 * comparison table — so the option costs no row of its own (it used to be a checkbox on a line above).
 *
 * The comparison needs a bounded Rechnungsdatum range to have a period at all; without one the caret is
 * disabled and its tooltip says why, which keeps the affordance and the reason in view without a row.
 */
export function InvoiceComparisonToggle({
  expanded,
  canCompare,
  onToggle,
}: {
  expanded: boolean;
  canCompare: boolean;
  onToggle: (on: boolean) => void;
}) {
  const t = useTranslations();
  return (
    <button
      type="button"
      disabled={!canCompare}
      onClick={() => onToggle(!expanded)}
      aria-label={t("fibu.rechnung.statistics.previousYearComparison")}
      aria-expanded={expanded}
      title={
        canCompare
          ? t("fibu.rechnung.statistics.previousYearComparison")
          : t("fibu.rechnung.statistics.previousYearComparisonHint")
      }
      className={cn(
        "-my-0.5 inline-flex items-center rounded p-0.5 align-middle",
        canCompare
          ? "cursor-pointer opacity-70 hover:opacity-100"
          : "cursor-default opacity-30"
      )}
    >
      <HugeiconsIcon
        icon={expanded ? ArrowDown01Icon : ArrowRight01Icon}
        size={16}
      />
    </button>
  );
}
