"use client";

import { AccessMatrixCell } from "./access-matrix-cell";
import { AccessMatrixPopover } from "./access-matrix-summary";
import { useAccessMatrixViewStore } from "./access-matrix-view-store";
import type { AccessEntryDto } from "../types";

/**
 * The `accessEntries` list column, switched by the page-wide view pill (see AccessListActions):
 * `detail` shows the full labelled matrix inline ([AccessMatrixCell]), `compact` (the default) the
 * single icon line with the same matrix a click away in a popover ([AccessMatrixPopover]).
 */
export function AccessMatrixColumnCell({
  entries,
}: {
  entries?: AccessEntryDto[] | null;
}) {
  const view = useAccessMatrixViewStore((s) => s.view);
  return view === "detail" ? (
    <AccessMatrixCell entries={entries} />
  ) : (
    <AccessMatrixPopover entries={entries} />
  );
}
