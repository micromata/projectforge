"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import { DataTable, DataTableColumnPanel } from "@/components/data-table";
import { logLevelRowClass } from "@/components/shared/log-level";
import { useFormatContext } from "@/hooks/use-format";
import type { LogFileEvent } from "@/lib/rs/admin-errors";
import type { ColumnState } from "@/components/data-table";
import { LogFileContext } from "./log-file-context";
import { logFileColumns, logFileRowId } from "./log-file-columns";
import type { OnLogger } from "./logger-menu";
import {
  useLogTable,
  useOpenContexts,
  useStoredLogColumns,
} from "./use-log-table";

/** The user prefs of the columns (`AdminLogViewerRest.logFileColumnStates`), shared by the search and a problem's occurrences. */
const GRID = "adminLogViewer/logFile";

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
 * Its columns can be resized, pinned and hidden, stored in the user's prefs; rendered once they have arrived.
 */
export function LogFileEntries(props: LogFileEntriesProps) {
  const stored = useStoredLogColumns(GRID);
  if (stored.pending) return null;
  return <LoadedEntries {...props} restoredState={stored.state} />;
}

function LoadedEntries({
  entries,
  terms = [],
  before,
  after,
  excludeLoggers = [],
  onLogger,
  restoredState,
}: LogFileEntriesProps & { restoredState: ColumnState }) {
  const t = useTranslations();
  const formatCtx = useFormatContext();
  const contexts = useOpenContexts();
  const columns = useMemo(
    () => logFileColumns({ t, formatCtx, terms, onLogger, context: contexts }),
    [t, formatCtx, terms, onLogger, contexts]
  );
  const { table, layout, resetColumns } = useLogTable<LogFileEvent>({
    grid: GRID,
    restoredState,
    columns,
    data: entries,
    getRowId: logFileRowId,
  });
  return (
    <div className="flex flex-col gap-1">
      <div className="flex justify-end">
        <DataTableColumnPanel table={table} onReset={resetColumns} />
      </div>
      <DataTable<LogFileEvent>
        table={table}
        columns={[]}
        data={[]}
        showPagination={false}
        dense
        autoHeight
        rowClassName={(row) => logLevelRowClass(row.level)}
        renderRowDetail={(row) =>
          contexts.isOpen(logFileRowId(row)) ? (
            <LogFileContext
              hit={row}
              terms={terms}
              before={before}
              after={after}
              excludeLoggers={excludeLoggers}
              onLogger={onLogger}
              layout={layout}
            />
          ) : null
        }
      />
    </div>
  );
}
