"use client";

import { useTranslations } from "next-intl";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Select, SelectTrigger } from "@/components/shared/copyable-select";
import { LOG_LEVEL_KEYS, LOG_THRESHOLDS } from "@/components/shared/log-level";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { SelectContent, SelectItem, SelectValue } from "@/components/ui/select";
import type { LogLevel } from "@/lib/rs/log-viewer";
import type { LogFileQueryFilter } from "@/lib/rs/log-file";
import { LogFilePeriodField } from "./log-file-period-field";
import { LoggerChips } from "./logger-chips";

/**
 * A change of the filter. [rerun]: a search already shown is repeated with it (level, period, categories),
 * otherwise it only applies to the next one (the text is searched by Enter, the context sizes by opening one).
 */
export type OnFilterChange = (
  patch: Partial<LogFileQueryFilter>,
  rerun: boolean
) => void;

/** The filter bar of the log file search: text, level, period, categories to show and to hide, context size. */
export function LogFileQueryFilterBar({
  filter,
  searching,
  onChange,
  onSearch,
}: {
  filter: LogFileQueryFilter;
  searching: boolean;
  onChange: OnFilterChange;
  onSearch: () => void;
}) {
  const t = useTranslations();
  const count = (value: string) =>
    Math.min(Math.max(Number(value) || 0, 0), 1000);
  return (
    <form
      className="space-y-2 px-4 pb-2 pt-3"
      onSubmit={(event) => {
        event.preventDefault();
        onSearch();
      }}
    >
      <div className="flex flex-wrap items-center gap-4">
        <Input
          aria-label={t("search._")}
          className="h-9 w-full max-w-md"
          placeholder={t(
            "system.admin.adminLogViewer.logFile.searchPlaceholder"
          )}
          value={filter.search ?? ""}
          onChange={(event) => onChange({ search: event.target.value }, false)}
        />
        <div className="flex items-center gap-2">
          <Label htmlFor="log-file-threshold">
            {t("system.admin.logViewer.level")}
          </Label>
          <Select
            value={filter.threshold}
            onValueChange={(threshold) =>
              onChange({ threshold: threshold as LogLevel }, true)
            }
          >
            <SelectTrigger id="log-file-threshold" className="h-9 w-32">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {LOG_THRESHOLDS.map((level) => (
                <SelectItem key={level} value={level}>
                  {t(LOG_LEVEL_KEYS[level])}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <LogFilePeriodField
          period={filter.period}
          from={filter.from}
          until={filter.until}
          // Entering a bound of a custom period runs no search for every keystroke.
          onChange={(patch) =>
            onChange(
              patch,
              patch.period !== undefined && patch.period !== "custom"
            )
          }
        />
        <Button type="submit" size="lg" disabled={searching}>
          {t("search._")}
        </Button>
      </div>
      <div className="flex flex-wrap items-center gap-x-6 gap-y-2">
        <LoggerChips
          id="log-file-include"
          label={t("system.admin.adminLogViewer.logFile.include")}
          hint={t("system.admin.adminLogViewer.logFile.includeTooltip")}
          placeholder={t(
            "system.admin.adminLogViewer.logFile.loggerPlaceholder"
          )}
          removeLabel={t("system.admin.adminLogViewer.logFile.remove")}
          value={filter.includeLoggers}
          onChange={(includeLoggers) => onChange({ includeLoggers }, true)}
        />
        <LoggerChips
          id="log-file-exclude"
          label={t("system.admin.adminLogViewer.logFile.exclude")}
          hint={t("system.admin.adminLogViewer.logFile.excludeTooltip")}
          placeholder={t(
            "system.admin.adminLogViewer.logFile.loggerPlaceholder"
          )}
          removeLabel={t("system.admin.adminLogViewer.logFile.remove")}
          value={filter.excludeLoggers}
          onChange={(excludeLoggers) => onChange({ excludeLoggers }, true)}
        />
        <div className="flex items-center gap-1.5">
          <HintTooltip
            text={t("system.admin.adminLogViewer.logFile.contextTooltip")}
          >
            <Label htmlFor="log-file-before">
              {t("system.admin.adminLogViewer.logFile.context")}
            </Label>
          </HintTooltip>
          <Input
            id="log-file-before"
            type="number"
            min={0}
            max={1000}
            className="h-7 w-16 text-xs"
            value={filter.before}
            onChange={(event) =>
              onChange({ before: count(event.target.value) }, false)
            }
          />
          <span className="text-xs">
            {t("system.admin.adminLogViewer.logFile.before")}
          </span>
          <Input
            aria-label={t("system.admin.adminLogViewer.logFile.after")}
            type="number"
            min={0}
            max={1000}
            className="h-7 w-16 text-xs"
            value={filter.after}
            onChange={(event) =>
              onChange({ after: count(event.target.value) }, false)
            }
          />
          <span className="text-xs">
            {t("system.admin.adminLogViewer.logFile.after")}
          </span>
        </div>
      </div>
    </form>
  );
}
