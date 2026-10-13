"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import {
  DataTable,
  DataTableColumnPanel,
  type ColumnState,
} from "@/components/data-table";
import { logLevelRowClass } from "@/components/shared/log-level";
import { useFormatContext } from "@/hooks/use-format";
import type { LogViewerEvent } from "@/lib/rs/log-viewer";
import { logMemoryColumns, logMemoryRowId } from "./log-memory-columns";
import { LogMemoryContext } from "./log-memory-context";
import { LOG_VIEWER_SORTING, logViewerColumns } from "./log-viewer-columns";
import {
  useLogTable,
  useOpenContexts,
  useStoredLogColumns,
} from "./use-log-table";

/** The user prefs of the admin's columns (`AdminLogViewerRest.columnStates`); the user view's aren't stored. */
const ADMIN_GRID = "adminLogViewer";

interface Props {
  /** The admin log viewer: logger column, the context of an entry, the columns stored in the user's prefs. */
  admin: boolean;
  entries: LogViewerEvent[];
  isFetching: boolean;
}

/**
 * The entries of the log viewer, latest first. Its columns can be resized, pinned and hidden; the admin's are
 * stored in the user's prefs (rendered once they have arrived), and every entry there opens its context below it
 * (the events before and after it in memory, see LogMemoryContext).
 */
export function LogViewerTable(props: Props) {
  const stored = useStoredLogColumns(props.admin ? ADMIN_GRID : undefined);
  if (stored.pending) return null;
  return <LoadedTable {...props} restoredState={stored.state} />;
}

function LoadedTable({
  admin,
  entries,
  isFetching,
  restoredState,
}: Props & { restoredState: ColumnState }) {
  const t = useTranslations();
  const formatCtx = useFormatContext();
  const contexts = useOpenContexts();
  const columns = useMemo(
    () =>
      admin
        ? logMemoryColumns({ t, formatCtx, context: contexts })
        : logViewerColumns(t, false),
    [admin, t, formatCtx, contexts]
  );
  const { table, layout, resetColumns } = useLogTable<LogViewerEvent>({
    grid: admin ? ADMIN_GRID : undefined,
    restoredState,
    columns,
    data: entries,
    getRowId: logMemoryRowId,
    initialSorting: LOG_VIEWER_SORTING,
  });
  return (
    <div className="flex min-h-0 flex-1 flex-col gap-1">
      <div className="flex justify-end">
        <DataTableColumnPanel table={table} onReset={resetColumns} />
      </div>
      <DataTable<LogViewerEvent>
        table={table}
        columns={[]}
        data={[]}
        isFetching={isFetching}
        showPagination={false}
        dense
        rowClassName={(row) => logLevelRowClass(row.level)}
        renderRowDetail={
          admin
            ? (row) =>
                contexts.isOpen(logMemoryRowId(row)) ? (
                  <LogMemoryContext event={row} layout={layout} />
                ) : null
            : undefined
        }
      />
    </div>
  );
}
