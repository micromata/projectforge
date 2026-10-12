"use client";

import { useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { DataTable } from "@/components/data-table";
import { logLevelRowClass } from "@/components/shared/log-level";
import { Button } from "@/components/ui/button";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import { useFormatContext } from "@/hooks/use-format";
import type { LogFileEvent } from "@/lib/rs/admin-errors";
import {
  fetchLogFileContext,
  type LogFileContextScope,
} from "@/lib/rs/log-file";
import { cn } from "@/lib/utils";
import { logFileColumns, logFileRowId } from "./log-file-columns";
import { TOGGLE_ON_CLASS } from "./log-source-switch";
import type { OnLogger } from "./logger-menu";

const SCOPE_KEYS: Record<LogFileContextScope, string> = {
  ALL: "system.admin.adminLogViewer.logFile.scope.all",
  USER: "system.admin.adminLogViewer.logFile.scope.user",
  LOGGER: "system.admin.adminLogViewer.logFile.scope.logger",
};

/**
 * The context of a hit: the entries before and after it in its file, read on opening and on every page step
 * ("older", "newer") or change of the scope. The scope narrows them to the hit's user or logger: the requests
 * of other users run at the same time and fill the neighbourhood of a hit with noise.
 */
export function LogFileContext({
  hit,
  terms,
  before: initialBefore,
  after: initialAfter,
  excludeLoggers,
  onLogger,
}: {
  hit: LogFileEvent;
  terms: string[];
  before: number;
  after: number;
  excludeLoggers: string[];
  onLogger?: OnLogger;
}) {
  const t = useTranslations();
  const formatCtx = useFormatContext();
  const [scope, setScope] = useState<LogFileContextScope>("ALL");
  const [before, setBefore] = useState(initialBefore);
  const [after, setAfter] = useState(initialAfter);
  const beforeStep = Math.max(initialBefore, 10);
  const afterStep = Math.max(initialAfter, 10);
  const request = {
    file: hit.file,
    line: hit.line,
    timestampMillis: hit.timestampMillis,
    user: hit.user ?? "",
    logger: hit.logger,
    before,
    after,
    scope,
    excludeLoggers,
  };
  const context = useQuery({
    queryKey: ["logFile", "context", request],
    queryFn: ({ signal }) => fetchLogFileContext(request, signal),
    placeholderData: keepPreviousData,
    // What was read stays valid (a file only grows); the hits themselves are refreshed by a new search.
    staleTime: Infinity,
  });
  const data = context.data;
  const rows = useMemo(
    () => (data?.found ? [...data.before, data.hit!, ...data.after] : []),
    [data]
  );
  // The jump in time from the entry before, shown in the time column.
  const columns = useMemo(() => {
    const gaps = new Map(
      rows.map((entry, index) => [
        logFileRowId(entry),
        index > 0 ? entry.timestampMillis - rows[index - 1].timestampMillis : 0,
      ])
    );
    return logFileColumns({ t, formatCtx, terms, onLogger, gaps });
  }, [rows, t, formatCtx, terms, onLogger]);
  const note = (text: string) => (
    <p className="px-2 text-muted-foreground">{text}</p>
  );

  return (
    <div className="space-y-1 border-l-4 border-primary/40 bg-muted/30 py-2 pl-2">
      <div className="flex flex-wrap items-center gap-2 px-2">
        <ToggleGroup
          type="single"
          variant="outline"
          size="sm"
          value={scope}
          onValueChange={(value) =>
            value && setScope(value as LogFileContextScope)
          }
        >
          {(Object.keys(SCOPE_KEYS) as LogFileContextScope[]).map((value) => (
            <ToggleGroupItem
              key={value}
              value={value}
              className={TOGGLE_ON_CLASS}
            >
              {t(SCOPE_KEYS[value])}
            </ToggleGroupItem>
          ))}
        </ToggleGroup>
        {context.isFetching && (
          <span className="text-muted-foreground">{t("loading")}</span>
        )}
      </div>
      {context.isError ? (
        <p className="px-2 text-destructive">{t("errorpage.title")}</p>
      ) : !data ? (
        note(t("loading"))
      ) : !data.found ? (
        note(t("system.admin.adminLogViewer.logFile.notFound"))
      ) : (
        <>
          <ContextEdge
            more={data.moreBefore}
            label={`↑ ${t("system.admin.adminLogViewer.logFile.older", { arg0: beforeStep })}`}
            none={t("system.admin.adminLogViewer.logFile.noOlder", {
              arg0: hit.file,
            })}
            disabled={context.isFetching}
            onClick={() => setBefore(before + beforeStep)}
          />
          <div className="rounded border bg-background">
            <DataTable<LogFileEvent>
              columns={columns}
              data={rows}
              enableColumnFilters={false}
              manualSorting={false}
              showPagination={false}
              dense
              autoHeight
              hideHeader
              getRowId={logFileRowId}
              rowClassName={(entry) =>
                cn(
                  entry === data.hit
                    ? "row-highlighted"
                    : "text-muted-foreground",
                  logLevelRowClass(entry.level)
                )
              }
            />
          </div>
          <ContextEdge
            more={data.moreAfter}
            label={`↓ ${t("system.admin.adminLogViewer.logFile.newer", { arg0: afterStep })}`}
            none={t("system.admin.adminLogViewer.logFile.noNewer", {
              arg0: hit.file,
            })}
            disabled={context.isFetching}
            onClick={() => setAfter(after + afterStep)}
          />
          {data.truncated &&
            note(t("system.admin.adminLogViewer.logFile.contextTruncated"))}
        </>
      )}
    </div>
  );
}

/** The begin or end of a context: a button paging on, or the note that the file has no more entries. */
function ContextEdge({
  more,
  label,
  none,
  disabled,
  onClick,
}: {
  more: boolean;
  label: string;
  none: string;
  disabled: boolean;
  onClick: () => void;
}) {
  return (
    <div className="px-2">
      {more ? (
        <Button size="xs" variant="ghost" disabled={disabled} onClick={onClick}>
          {label}
        </Button>
      ) : (
        <span className="text-[10px] text-muted-foreground">{none}</span>
      )}
    </div>
  );
}
