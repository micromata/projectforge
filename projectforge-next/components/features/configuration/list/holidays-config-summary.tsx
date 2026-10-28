"use client";

import { useTranslations } from "next-intl";
import { useFormatContext } from "@/hooks/use-format";
import { formatNumber } from "@/lib/format";
import { HOLIDAY_DEFINITIONS, parseHolidaysConfig } from "../holidays-config";

/** The configured holidays in one line, for the value column of the list: own ones and changed pre-defined ones. */
export function HolidaysConfigSummary({ json }: { json: string | null | undefined }) {
  const t = useTranslations();
  const format = useFormatContext();
  const { entries, invalid } = parseHolidaysConfig(json);
  if (invalid) {
    return (
      <span className="text-warning">
        {t("administration.configuration.holidays.invalidJson")}
      </span>
    );
  }
  const names = entries.map((entry) => {
    const definition = HOLIDAY_DEFINITIONS.find((def) => def.id === entry.id);
    const name = definition ? t(definition.i18nKey) : entry.label;
    if (entry.ignore) {
      return `${name} (${t("administration.configuration.holidays.ignored")})`;
    }
    return entry.workFraction != null
      ? `${name} (${formatNumber(entry.workFraction, format)})`
      : name;
  });
  return <span className="line-clamp-2">{names.join(", ")}</span>;
}
