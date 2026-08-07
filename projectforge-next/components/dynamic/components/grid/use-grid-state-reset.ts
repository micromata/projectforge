"use client";

import { useCallback } from "react";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import type { TableStateResult } from "@/components/data-table";
import { callDynamicAction } from "@/lib/rs/dynamic";
import type { AgGridNode } from "@/lib/dynamic/grid/ag-grid-types";
import { initialStateFrom } from "@/lib/dynamic/grid/initial-state";

/**
 * "Reset columns" for a dynamic grid.
 *
 * The endpoint (`AbstractPagesRest.resetGridState`) drops the user's stored grid
 * preference and answers with an UPDATE action carrying the default `columnDefs`
 * and `sortModel`. That answer is applied to the table state here rather than
 * handed to `callAction`: the generic interpreter would file both under
 * `variables`, where nothing reads them — the grid takes its columns from the
 * layout node, not from a variable.
 *
 * Without the url (a grid that doesn't persist its state) the table still resets
 * to the column defs' own defaults, which is what the empty state means.
 */
export function useGridStateReset(
  url: string | undefined,
  state: TableStateResult
) {
  const t = useTranslations("table");
  const apply = useCallback(
    (columnState: ReturnType<typeof initialStateFrom>) => {
      state.setSorting(columnState.sorting ?? []);
      state.setColumnVisibility(columnState.columnVisibility ?? {});
      state.setColumnPinning(columnState.columnPinning ?? {});
      state.setColumnSizing(columnState.columnSizing ?? {});
      state.setColumnOrder(columnState.columnOrder ?? []);
      state.setColumnFilters([]);
    },
    [state]
  );

  return useCallback(async () => {
    if (!url) {
      apply(initialStateFrom({}));
      return;
    }
    try {
      // A GET carries no body at all (see callDynamicAction), so the postData is
      // only there to satisfy the signature.
      const result = await callDynamicAction("GET", url, { data: {} });
      if (result.kind !== "action") return;
      const variables = result.response.variables ?? {};
      apply(
        initialStateFrom({
          columnDefs: variables.columnDefs as AgGridNode["columnDefs"],
          sortModel: variables.sortModel as AgGridNode["sortModel"],
        })
      );
    } catch {
      // The stored state may or may not be gone now, so say the reset failed
      // rather than leaving the columns silently unchanged.
      toast.error(t("resetFailed"));
    }
  }, [apply, t, url]);
}
