"use client";

import type { Table } from "@tanstack/react-table";
import { useMutation } from "@tanstack/react-query";
import { fetchListPage } from "@/lib/rs/client";
import { MAX_MASS_UPDATE } from "@/lib/rs/multi-select";
import type { MagicFilter } from "@/lib/rs/types";
import { toast } from "@/lib/toast";
import type { ListRow } from "@/hooks/use-entity-list-page";

/**
 * Ticks the first MAX_MASS_UPDATE rows of the result as the list orders them, instead of the current
 * ticks — the most one mass update changes (see the selection bar's "select first" button).
 *
 * Answers `undefined` while the result has no more rows than that: the button would then mean the
 * same as select all. A list paged on the server holds only its current page, so the ids are asked
 * of the server then — the first page of [MAX_MASS_UPDATE] rows under the same filter and sort.
 */
export function useSelectFirst<Row extends ListRow>(
  entity: string,
  table: Table<Row>,
  filter: MagicFilter
): (() => void) | undefined {
  const select = useMutation({
    mutationFn: async (): Promise<string[]> => {
      if (!table.options.manualPagination) {
        return table
          .getPrePaginationRowModel()
          .rows.slice(0, MAX_MASS_UPDATE)
          .map((row) => row.id);
      }
      // doNotStore: a side request, the list's own call is what stores the user's current filter.
      const result = await fetchListPage<Row>(
        entity,
        filter,
        0,
        MAX_MASS_UPDATE,
        false,
        undefined,
        true
      );
      return result.resultSet.map((row) => String(row.id));
    },
    onSuccess: (ids) =>
      table.setRowSelection(Object.fromEntries(ids.map((id) => [id, true]))),
    onError: (error) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });
  if (table.getRowCount() <= MAX_MASS_UPDATE) return undefined;
  return () => select.mutate();
}
