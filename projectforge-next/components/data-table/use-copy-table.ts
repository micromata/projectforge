"use client";

import { useCallback, useState } from "react";
import { useTranslations } from "next-intl";
import type { Row, Table } from "@tanstack/react-table";
import { useFormatContext } from "@/hooks/use-format";
import { formatValue } from "@/lib/format";
import { toast } from "@/lib/toast";
import { fetchList } from "@/lib/rs/client";
import type { MagicFilter } from "@/lib/rs/types";
import { SELECTION_COLUMN_ID } from "./selection-column";
import { buildTableText, writeTableToClipboard } from "./table-clipboard";

function plainText(value: unknown): string {
  if (value == null) return "";
  if (Array.isArray(value)) return value.map(plainText).join(", ");
  if (typeof value === "object" && "displayName" in value)
    return String((value as { displayName?: unknown }).displayName ?? "");
  return String(value);
}

/**
 * Copies the table — header row and every row of the current filter, in the order and with the columns
 * shown — to the clipboard as a table that pastes into Excel or Numbers (see [writeTableToClipboard]).
 *
 * A list that pages on the server holds one page only, so there the whole result set is fetched first.
 */
export function useCopyTable<Row_>({
  table,
  entity,
  filter,
}: {
  table: Table<Row_>;
  entity: string;
  filter: MagicFilter;
}): { copy: () => Promise<void>; isPending: boolean } {
  const t = useTranslations();
  const format = useFormatContext();
  const [isPending, setBusy] = useState(false);

  const copy = useCallback(async () => {
    setBusy(true);
    try {
      const columns = table
        .getVisibleLeafColumns()
        .filter((c) => c.id !== SELECTION_COLUMN_ID && c.columnDef.meta?.label);
      let data: Row_[];
      let truncated = false;
      if (table.options.manualPagination) {
        const result = await fetchList<Row_>(entity, filter, undefined, true);
        data = result.resultSet;
        truncated = result.resultSetTruncated ?? false;
      } else {
        // Filtered and sorted, but not cut to the page on screen.
        data = table
          .getPrePaginationRowModel()
          .rows.map((r: Row<Row_>) => r.original);
      }
      const rows = data.map((original, index) =>
        columns.map((column) => {
          const value = column.accessorFn?.(original, index);
          const exportText = column.columnDef.meta?.exportText;
          if (exportText) return exportText(value, original);
          // A column of the server's layout: its formatter name decides how the value reads.
          const spec = column.columnDef.meta?.cellSpec;
          if (spec?.kind === "text" && spec.format && value != null)
            return formatValue(value, spec.format, format);
          return plainText(value);
        })
      );
      await writeTableToClipboard(
        buildTableText(
          columns.map((c) => c.columnDef.meta?.label ?? c.id),
          rows
        )
      );
      toast.success(t("table.copy.done", { arg0: rows.length }));
      if (truncated) toast.warning(t("table.copy.truncated"));
    } catch {
      toast.error(t("table.copy.failed"));
    } finally {
      setBusy(false);
    }
  }, [table, entity, filter, format, t]);
  return { copy, isPending };
}
