"use client";

import { useMemo } from "react";
import { DataTable } from "@/components/data-table";
import { logLevelRowClass } from "@/components/shared/log-level";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import type { LogContextData, LogContextProps, LogEntry } from "./log-context";
import { useContextTable } from "./use-log-table";

/** The entries of a context in the layout of the table they belong to, the entry itself marked. */
export function ContextTable<E extends LogEntry>({
  data,
  columns: columnsOf,
  layout,
  getRowId,
}: LogContextProps<E> & { data: LogContextData<E> }) {
  const rows = useMemo(
    () => (data.found ? [...data.before, data.hit!, ...data.after] : []),
    [data]
  );
  const columns = useMemo(() => {
    const gaps = new Map(
      rows.map((entry, index) => [
        getRowId(entry),
        index > 0 ? entry.timestampMillis - rows[index - 1].timestampMillis : 0,
      ])
    );
    return columnsOf(gaps);
  }, [rows, columnsOf, getRowId]);
  const table = useContextTable({ layout, columns, data: rows, getRowId });
  return (
    <div className="rounded border bg-background">
      <DataTable<E>
        table={table}
        columns={[]}
        data={[]}
        showPagination={false}
        dense
        autoHeight
        hideHeader
        rowClassName={(entry) =>
          cn(
            entry === data.hit ? "row-highlighted" : "text-muted-foreground",
            logLevelRowClass(entry.level)
          )
        }
      />
    </div>
  );
}

/** The begin or end of a context: a button paging on, or the note that there are no more entries. */
export function ContextEdge({
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
