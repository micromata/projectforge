"use client";

import { useTranslations } from "next-intl";
import { HighlightedText } from "@/components/shared/highlighted-text";
import { richTextToPlainText } from "@/components/shared/rich-text";
import { parseForecastConfig } from "../forecast-config";

/** The settings of the forecast in one line, for the value column of the list. */
export function ForecastConfigSummary({
  json,
  highlight,
}: {
  json: string | null | undefined;
  highlight?: string;
}) {
  const t = useTranslations("fibu.auftrag.forecast.config");
  const { config, invalid } = parseForecastConfig(json);
  if (invalid) {
    return <span className="text-warning">{t("invalidJson")}</span>;
  }
  const hint = richTextToPlainText(config.planningDateHint ?? "");
  if (!hint) return null;
  return (
    <span className="line-clamp-2">
      <span className="text-muted-foreground">{t("planningDateHint._")}: </span>
      <HighlightedText text={hint} query={highlight} />
    </span>
  );
}
