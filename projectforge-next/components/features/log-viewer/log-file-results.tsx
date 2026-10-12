"use client";

import { useTranslations } from "next-intl";
import type { UseQueryResult } from "@tanstack/react-query";
import { useFormatContext } from "@/hooks/use-format";
import { formatNumber } from "@/lib/format";
import type { LogFileSearchData } from "@/lib/rs/admin-errors";
import type { LogFileQueryFilter } from "@/lib/rs/log-file";
import { LogFileEntries } from "./log-file-entries";
import { LogFileFormatWarning } from "./log-file-format-warning";
import { searchTerms } from "./log-file-text";
import type { OnLogger } from "./logger-menu";

/** The answer of a log file search: what was searched, its notes and the hits with their context. */
export function LogFileResults({
  submitted,
  result,
  before,
  after,
  onLogger,
}: {
  submitted: LogFileQueryFilter | null;
  result: UseQueryResult<LogFileSearchData>;
  /** The context sizes a context opened from now on starts with. */
  before: number;
  after: number;
  onLogger: OnLogger;
}) {
  const t = useTranslations();
  const formatCtx = useFormatContext();
  const data = result.data;
  const note = (text: string) => (
    <p className="text-muted-foreground">{text}</p>
  );
  return (
    <div className="flex min-h-0 flex-1 flex-col gap-2 overflow-y-auto px-4 pb-4 text-sm">
      {!submitted ? (
        note(t("system.admin.adminLogViewer.logFile.start"))
      ) : result.isError ? (
        <p className="text-destructive">{t("errorpage.title")}</p>
      ) : !data ? (
        note(t("loading"))
      ) : (
        <>
          <LogFileFormatWarning data={data} />
          {note(
            `${t("system.admin.adminLogViewer.logFile.hits", {
              arg0: formatNumber(data.entries.length, formatCtx),
            })} · ${t("system.admin.adminErrors.logFile.files", {
              arg0: data.searchedFiles.join(", ") || "-",
            })}`
          )}
          {data.truncated &&
            note(t("system.admin.adminLogViewer.logFile.truncated"))}
          {data.entries.length === 0 ? (
            note(t("system.admin.adminLogViewer.logFile.none"))
          ) : (
            <LogFileEntries
              // Opened contexts belong to the hits of one search.
              key={JSON.stringify(submitted)}
              entries={data.entries}
              terms={searchTerms(submitted.search)}
              before={before}
              after={after}
              excludeLoggers={submitted.excludeLoggers}
              onLogger={onLogger}
            />
          )}
        </>
      )}
    </div>
  );
}
