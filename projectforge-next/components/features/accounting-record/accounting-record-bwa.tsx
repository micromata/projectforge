"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowDown01Icon, ArrowRight01Icon } from "@hugeicons/core-free-icons";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency, formatNumber } from "@/lib/format";
import { leafKeyOf } from "@/lib/leaf-key";
import { cn } from "@/lib/utils";
import type { BwaRow, BwaStatistics } from "./types";

/**
 * The BWA (Betriebswirtschaftliche Auswertung / business assessment) of the current result set, shown above the
 * accounting-record list — the analog of the invoice `InvoiceStatisticsLine`. Three headline figures are always
 * visible; a toggle reveals the full assessment table. Amounts are formatted locally (raw amounts + scale/unit
 * come from the backend), never the legacy server-rendered HTML.
 */
export function AccountingRecordBwa({
  statistics,
  isFetching,
  className,
}: {
  statistics: BwaStatistics | undefined;
  isFetching?: boolean;
  className?: string;
}) {
  const t = useTranslations();
  const format = useFormatContext();
  const [open, setOpen] = useState(false);

  if (!statistics || statistics.rows.length === 0) {
    return null;
  }

  const headlines: { labelKey: string; value?: number | null }[] = [
    {
      labelKey: "fibu.businessAssessment.overallPerformance",
      value: statistics.overallPerformance,
    },
    {
      labelKey: "fibu.businessAssessment.merchandisePurchase",
      value: statistics.merchandisePurchase,
    },
    {
      labelKey: "fibu.businessAssessment.preliminaryResult",
      value: statistics.preliminaryResult,
    },
  ];

  // Blank for a null/zero amount, exactly as the legacy BusinessAssessment.asHtml does (spacer/grouping rows
  // carry no value and stay empty).
  const rowAmount = (row: BwaRow) => {
    if (row.amount == null || row.amount === 0) return "";
    const number = formatNumber(row.amount, format, row.scale);
    return row.unit ? `${number} ${row.unit}` : number;
  };
  const headlineAmount = (value?: number | null) =>
    value != null && value !== 0 ? formatCurrency(value, format) : "";

  return (
    <Collapsible
      open={open}
      onOpenChange={setOpen}
      className={cn(
        // "Zettel" look: a small sheet detached from the list — inset horizontally, extra bottom margin,
        // rounded card with a soft shadow. overflow-hidden clips the hover state and the table's top border.
        "mx-3 mt-2 mb-4 overflow-hidden rounded-md border bg-card text-xs shadow-sm",
        isFetching && "opacity-60",
        className
      )}
    >
      {/* Compact headline line, matching the legacy BWA: `Label: value` joined by ` | `, the overall
          performance highlighted. The whole line toggles the full assessment table. */}
      <CollapsibleTrigger className="flex w-full flex-wrap items-baseline gap-x-2 gap-y-0.5 px-3 py-1.5 text-left hover:bg-muted/40">
        <HugeiconsIcon
          icon={open ? ArrowDown01Icon : ArrowRight01Icon}
          size={14}
          className="self-center text-muted-foreground"
        />
        {headlines.map((headline, index) => (
          <span key={headline.labelKey} className="flex items-baseline gap-x-2">
            {index > 0 && (
              <span className="text-muted-foreground" aria-hidden>
                |
              </span>
            )}
            <span
              className={cn(
                "flex items-baseline gap-1.5",
                index === 0 && "text-primary"
              )}
            >
              <span className={cn(index !== 0 && "text-muted-foreground")}>
                {t(headline.labelKey)}:
              </span>
              <span className="font-semibold tabular-nums">
                {headlineAmount(headline.value)}
              </span>
            </span>
          </span>
        ))}
      </CollapsibleTrigger>
      <CollapsibleContent>
        {/* Auto-width table so the amount sits directly after the (fixed-width) title column instead of being
            pushed to the far right; amounts still right-align within their own column so decimals line up. */}
        <table className="w-auto border-t text-[11px] leading-tight">
          <caption className="px-3 pt-1 text-left font-medium text-muted-foreground">
            {t(leafKeyOf("fibu.businessAssessment", t.has))}
          </caption>
          <tbody>
            {statistics.rows.map((row, index) => {
              const emphasized = row.indent === 0 && !!row.title;
              return (
                <tr key={row.id ?? row.no ?? index}>
                  <td className="w-14 px-3 py-0.5 text-muted-foreground tabular-nums">
                    {row.no}
                  </td>
                  <td
                    className={cn(
                      "w-64 px-2 py-0.5",
                      emphasized && "font-semibold"
                    )}
                    style={{ paddingLeft: `${0.5 + row.indent}rem` }}
                  >
                    {row.title}
                  </td>
                  <td
                    className={cn(
                      "px-3 py-0.5 text-right tabular-nums whitespace-nowrap",
                      emphasized && "font-semibold"
                    )}
                  >
                    {rowAmount(row)}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </CollapsibleContent>
    </Collapsible>
  );
}
