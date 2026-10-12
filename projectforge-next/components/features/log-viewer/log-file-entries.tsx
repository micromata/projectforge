"use client";

import { useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import { DataTable } from "@/components/data-table";
import { logLevelRowClass } from "@/components/shared/log-level";
import { useFormatContext } from "@/hooks/use-format";
import type { LogFileEvent } from "@/lib/rs/admin-errors";
import { LogFileContext } from "./log-file-context";
import { logFileColumns, logFileRowId } from "./log-file-columns";
import type { OnLogger } from "./logger-menu";

export interface LogFileEntriesProps {
  /** The hits, as answered by the search (newest first). */
  entries: LogFileEvent[];
  /** The words searched for, highlighted in the messages. */
  terms?: string[];
  /** The number of entries before and after a hit its context starts with, and the step to page further. */
  before: number;
  after: number;
  /** The loggers the context leaves out, as the search does. */
  excludeLoggers?: string[];
  /** Offered on every entry's logger; without, the logger is plain text. */
  onLogger?: OnLogger;
}

/**
 * The hits of a search of the log files, each with a button opening its context in place below it (the
 * entries before and after it in its file, as `grep -C`, see LogFileContext and DataTable.renderRowDetail).
 */
export function LogFileEntries({
  entries,
  terms = [],
  before,
  after,
  excludeLoggers = [],
  onLogger,
}: LogFileEntriesProps) {
  const t = useTranslations();
  const formatCtx = useFormatContext();
  const [open, setOpen] = useState<ReadonlySet<string>>(new Set());
  const columns = useMemo(
    () =>
      logFileColumns({
        t,
        formatCtx,
        terms,
        onLogger,
        context: {
          isOpen: (id) => open.has(id),
          toggle: (id) => {
            const next = new Set(open);
            if (!next.delete(id)) next.add(id);
            setOpen(next);
          },
        },
      }),
    [t, formatCtx, terms, onLogger, open]
  );
  return (
    <DataTable<LogFileEvent>
      columns={columns}
      data={entries}
      enableColumnFilters={false}
      manualSorting={false}
      showPagination={false}
      dense
      autoHeight
      getRowId={logFileRowId}
      rowClassName={(row) => logLevelRowClass(row.level)}
      renderRowDetail={(row) =>
        open.has(logFileRowId(row)) ? (
          <LogFileContext
            hit={row}
            terms={terms}
            before={before}
            after={after}
            excludeLoggers={excludeLoggers}
            onLogger={onLogger}
          />
        ) : null
      }
    />
  );
}
