"use client";

import { useMemo } from "react";
import { DataTable } from "@/components/data-table";
import type { ListRow } from "@/hooks/use-entity-list-page";
import { useListMeta } from "@/hooks/use-list-meta";
import type { EntityMetadata } from "@/lib/metadata/types";
import {
  activeFilterFields,
  revealedColumnsOf,
  visibleColumnsOf,
} from "@/lib/page-def/define-page";
import type { ColumnDeclaration } from "@/lib/page-def/types";
import { useDeclaredColumns } from "./use-declared-columns";

/**
 * The picked entries as the list's own rows — the table inside [SelectedEntriesPanel].
 *
 * Its own file because the columns are built by a hook, which may not run inside the panel's
 * collapsed branch: nothing is fetched until the panel is open, and a hook cannot be conditional.
 *
 * Rendered from the same declarations as the list (see useDeclaredColumns, which takes the
 * translations and the user's formats itself), so a row reads here exactly as it does there. With no
 * state of its own: this is a look at rows the user already picked, so there is nothing to sort,
 * filter, hide or reorder, and nothing worth remembering per user.
 *
 * Given the list's [entity], the declarations are narrowed as the list narrows them (see DeclaredList):
 * to the columns this installation and user have, and to those the list's filter reveals — the remembered
 * one, which is the filter the selection was made under. Without it, `columns` is taken as already
 * narrowed: the list's own selection bar passes its derived set, which follows the filter the list
 * actually runs (on a transient jump that is not the remembered one).
 */
export function SelectedEntriesTable<
  Row extends ListRow,
  M extends EntityMetadata,
>({
  entity,
  metadata,
  columns,
  rows,
  isLoading,
}: {
  entity?: string;
  metadata: M;
  columns: ColumnDeclaration<Row, M>[];
  rows: Row[];
  isLoading?: boolean;
}) {
  // Already in the cache: the mass update page is only reached from the list.
  const listMeta = useListMeta(entity ?? "", { enabled: !!entity }).data;
  const filter = listMeta?.filter;
  const variables = listMeta?.variables;
  const activeFilterKey = useMemo(
    () => [...activeFilterFields(filter)].sort().join(" "),
    [filter]
  );
  const shown = useMemo(
    () =>
      entity
        ? revealedColumnsOf(
            visibleColumnsOf(columns, variables),
            new Set(activeFilterKey ? activeFilterKey.split(" ") : [])
          )
        : columns,
    [entity, columns, variables, activeFilterKey]
  );
  const declared = useDeclaredColumns<Row, M>(metadata, shown);
  return (
    // Bounded rather than as tall as the selection: a hundred rows would otherwise push the fields
    // this panel sits above off the screen. The table scrolls inside, as it does in the list.
    <div className="flex max-h-96 flex-col">
      <DataTable<Row>
        columns={declared}
        data={rows}
        isLoading={isLoading}
        enableColumnFilters={false}
        manualSorting={false}
        getRowId={(row) => String(row.id)}
      />
    </div>
  );
}
