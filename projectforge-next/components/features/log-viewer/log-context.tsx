"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import type { LogContextScope, LogLevel } from "@/lib/rs/log-viewer";
import { TOGGLE_ON_CLASS } from "./log-source-switch";
import { ContextEdge, ContextTable } from "./log-context-parts";
import type { LogColumnLayout } from "./use-log-table";

const SCOPE_KEYS: Record<LogContextScope, string> = {
  ALL: "system.admin.adminLogViewer.logFile.scope.all",
  USER: "system.admin.adminLogViewer.logFile.scope.user",
  LOGGER: "system.admin.adminLogViewer.logFile.scope.logger",
};

/** The answer of a context endpoint: an entry with its neighbours, oldest first. */
export interface LogContextData<E> {
  found: boolean;
  before: E[];
  hit?: E | null;
  after: E[];
  moreBefore: boolean;
  moreAfter: boolean;
  truncated: boolean;
}

export interface LogEntry {
  timestampMillis: number;
  level: LogLevel;
}

export interface LogContextProps<E extends LogEntry> {
  /** Identifies the entry in the query cache, e.g. its file and line. */
  queryKey: unknown[];
  fetch: (
    request: { before: number; after: number; scope: LogContextScope },
    signal: AbortSignal
  ) => Promise<LogContextData<E>>;
  /** The number of entries before and after it the context starts with, and the step to page further. */
  before: number;
  after: number;
  /** The columns of the table the entry is in, given the jump in time from the entry before per row id. */
  columns: (gaps: Map<string, number>) => ColumnDef<E, unknown>[];
  /** The layout of that table, so the columns line up with it. */
  layout: LogColumnLayout;
  getRowId: (entry: E) => string;
  /** The notes at the beginning and end of the entries there are (the file's, the memory's). */
  noOlder: string;
  noNewer: string;
  /** The entry isn't there anymore. */
  notFound: string;
}

/**
 * The context of an entry of a log table, shown below it (DataTable.renderRowDetail): the entries before and
 * after it, read on opening and on every page step ("older", "newer") or change of the scope. The scope narrows
 * them to the entry's user or logger: the requests of other users run at the same time and fill the
 * neighbourhood of an entry with noise. Shared by the log files and the memory (LogFileContext,
 * LogMemoryContext).
 */
export function LogContext<E extends LogEntry>(props: LogContextProps<E>) {
  const t = useTranslations();
  const [scope, setScope] = useState<LogContextScope>("ALL");
  const [before, setBefore] = useState(props.before);
  const [after, setAfter] = useState(props.after);
  const beforeStep = Math.max(props.before, 10);
  const afterStep = Math.max(props.after, 10);
  const context = useQuery({
    queryKey: [...props.queryKey, before, after, scope],
    queryFn: ({ signal }) => props.fetch({ before, after, scope }, signal),
    placeholderData: keepPreviousData,
    // What was read stays valid; newer entries are read by paging on.
    staleTime: Infinity,
  });
  const data = context.data;
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
          onValueChange={(value) => value && setScope(value as LogContextScope)}
        >
          {(Object.keys(SCOPE_KEYS) as LogContextScope[]).map((value) => (
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
        // Not reached within the time limit (a hit far back in a big file) rather than gone.
        note(
          data.truncated
            ? t("system.admin.adminLogViewer.logFile.contextTruncated")
            : props.notFound
        )
      ) : (
        <>
          <ContextEdge
            more={data.moreBefore}
            label={`↑ ${t("system.admin.adminLogViewer.logFile.older", { arg0: beforeStep })}`}
            none={props.noOlder}
            disabled={context.isFetching}
            onClick={() => setBefore(before + beforeStep)}
          />
          <ContextTable data={data} {...props} />
          <ContextEdge
            more={data.moreAfter}
            label={`↓ ${t("system.admin.adminLogViewer.logFile.newer", { arg0: afterStep })}`}
            none={props.noNewer}
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
