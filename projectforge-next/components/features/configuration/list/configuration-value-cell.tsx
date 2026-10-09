"use client";

import { useTranslations } from "next-intl";
import { BooleanCell } from "@/components/data-table/cells/boolean-cell";
import { HighlightedText } from "@/components/shared/highlighted-text";
import { useFormatContext } from "@/hooks/use-format";
import { formatNumber, formatPercentageDecimal } from "@/lib/format";
import { CONTRIBUTION_MARGIN_PARAM } from "../contribution-margin-config";
import { FORECAST_PARAM } from "../forecast-config";
import type { ConfigurationRow } from "../types";
import { ContributionMarginConfigSummary } from "./contribution-margin-config-summary";
import { ForecastConfigSummary } from "./forecast-config-summary";

/**
 * The current value of a configuration parameter, read-only, formatted for the type it is stored as —
 * the "Wert" column of the legacy `ConfigurationListPage`. A tick for a boolean, the user's number
 * format for a count, a percentage for a factor, the id or text for everything else. The value slot
 * follows the type (see Configuration.copyTo), so an unset slot renders as nothing.
 */
export function ConfigurationValueCell({
  row,
  highlight,
}: {
  row: ConfigurationRow;
  highlight?: string;
}) {
  const t = useTranslations();
  const format = useFormatContext();

  switch (row.configurationType) {
    case "BOOLEAN":
      return <BooleanCell value={row.booleanValue === true} t={t} />;
    case "LONG":
    case "INTEGER":
      return (
        <span className="tabular-nums">
          {formatNumber(row.longValue, format)}
        </span>
      );
    case "FLOAT":
      return (
        <span className="tabular-nums">
          {formatNumber(row.floatValue, format, 2)}
        </span>
      );
    case "PERCENT":
      return (
        <span className="tabular-nums">
          {formatPercentageDecimal(row.floatValue, format)}
        </span>
      );
    case "JSON":
      if (row.parameter === CONTRIBUTION_MARGIN_PARAM) {
        return <ContributionMarginConfigSummary json={row.stringValue} />;
      }
      if (row.parameter === FORECAST_PARAM) {
        return (
          <ForecastConfigSummary json={row.stringValue} highlight={highlight} />
        );
      }
      return (
        <span className="line-clamp-2 break-all font-mono text-xs">
          <HighlightedText text={row.stringValue ?? ""} query={highlight} />
        </span>
      );
    // STRING, TEXT, TIME_ZONE and the unused CALENDAR/TASK all carry their value as text.
    default:
      return (
        <span>
          <HighlightedText text={row.stringValue ?? ""} query={highlight} />
        </span>
      );
  }
}
