"use client";

import { DataTable } from "@/components/data-table";
import type { ListRow } from "@/hooks/use-entity-list-page";
import type { EntityMetadata } from "@/lib/metadata/types";
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
 */
export function SelectedEntriesTable<
  Row extends ListRow,
  M extends EntityMetadata,
>({
  metadata,
  columns,
  rows,
  isLoading,
}: {
  metadata: M;
  columns: ColumnDeclaration<Row, M>[];
  rows: Row[];
  isLoading?: boolean;
}) {
  const declared = useDeclaredColumns<Row, M>(metadata, columns);
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
