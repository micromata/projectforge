"use client";

import { useCallback, useRef, useState } from "react";
import type { Table } from "@tanstack/react-table";
import { SELECTION_COLUMN_ID, useRowSelection } from "@/components/data-table";
import type { LogGroupEntry } from "@/lib/rs/admin-errors";

/**
 * The selection mode of the problems table (see AdminErrorsTable): whether it is on, which problems are picked
 * and the table options that go with it. The ranges are taken over the rows as displayed (sorted and filtered),
 * read when the click happens: the table is built after the selection, so the caller puts it into [tableRef]
 * (as in useEntityListPage).
 */
export function useProblemSelection() {
  const [active, setActive] = useState(false);
  const tableRef = useRef<Table<LogGroupEntry> | null>(null);
  const displayedRowIds = useCallback(
    () => (tableRef.current?.getRowModel().rows ?? []).map((row) => row.id),
    []
  );
  const selection = useRowSelection(displayedRowIds);
  const { setState, clear } = selection;
  const selectAll = useCallback(
    () =>
      setState(Object.fromEntries(displayedRowIds().map((id) => [id, true]))),
    [setState, displayedRowIds]
  );
  const leave = () => {
    clear();
    setActive(false);
  };
  return {
    active,
    toggle: () => (active ? leave() : setActive(true)),
    leave,
    selectAll,
    selection,
    tableRef,
    tableOptions: {
      lockedColumnIds: active ? [SELECTION_COLUMN_ID] : undefined,
      rowSelection: selection.state,
      onRowSelectionChange: selection.setState,
      enableRowSelection: active,
    },
  };
}
