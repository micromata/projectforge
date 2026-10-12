"use client";

import { useTranslations } from "next-intl";
import { DateTimeInput } from "@/components/shared/date-time-input";
import { Select, SelectTrigger } from "@/components/shared/copyable-select";
import { Label } from "@/components/ui/label";
import { SelectContent, SelectItem, SelectValue } from "@/components/ui/select";
import { DEFAULT_TO_TIME } from "@/lib/user-zone";
import {
  CUSTOM_PERIOD,
  DEFAULT_PERIOD,
  LOG_FILE_PRESETS,
} from "./log-file-period";

/** The period of the log file search: a rolling preset ending now, or two entered bounds. */
export function LogFilePeriodField({
  period,
  from,
  until,
  onChange,
}: {
  period: string | null | undefined;
  from: number | null | undefined;
  until: number | null | undefined;
  onChange: (patch: {
    period?: string;
    from?: number | null;
    until?: number | null;
  }) => void;
}) {
  const t = useTranslations();
  const tSearch = useTranslations("search");
  const value = period ?? DEFAULT_PERIOD;
  const iso = (millis: number | null | undefined) =>
    millis == null ? null : new Date(millis).toISOString();
  const millis = (value: string | null) => (value ? Date.parse(value) : null);
  return (
    <div className="flex flex-wrap items-center gap-2">
      <Label htmlFor="log-file-period">{t("filter.period")}</Label>
      <Select
        value={value}
        onValueChange={(next) => onChange({ period: next })}
      >
        <SelectTrigger id="log-file-period" className="h-9 w-44">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          {LOG_FILE_PRESETS.map((preset) => (
            <SelectItem key={preset.id} value={preset.id}>
              {tSearch(
                preset.key,
                preset.arg == null ? {} : { arg0: preset.arg }
              )}
            </SelectItem>
          ))}
          <SelectItem value={CUSTOM_PERIOD}>
            {t("system.admin.adminLogViewer.logFile.period.custom")}
          </SelectItem>
        </SelectContent>
      </Select>
      {value === CUSTOM_PERIOD && (
        <>
          <DateTimeInput
            dateLabel={t("filter.dateFrom")}
            timeLabel={t("filter.timeFrom")}
            value={iso(from)}
            onChange={(next) => onChange({ from: millis(next) })}
          />
          <span className="text-muted-foreground">–</span>
          <DateTimeInput
            dateLabel={t("filter.dateTo")}
            timeLabel={t("filter.timeTo")}
            fallbackTime={DEFAULT_TO_TIME}
            value={iso(until)}
            onChange={(next) => onChange({ until: millis(next) })}
          />
        </>
      )}
    </div>
  );
}
