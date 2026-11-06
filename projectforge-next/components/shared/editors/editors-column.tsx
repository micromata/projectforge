"use client";

import { Badge } from "@/components/ui/badge";
import type { ComputedColumn } from "@/lib/page-def/types";

/** What a list row of an entity with the editors column carries (HistoryEditorsService.fillEditors). */
export interface RowWithEditors {
  /** The names of everybody who created or changed the entry, in the order of their first change. */
  editors?: string[] | null;
}

/**
 * The column with a pill for everybody who created or changed the entry at some point, declarable by every
 * list whose backend fills `editors` (HistoryEditorsService.fillEditors).
 *
 * Not sortable and without a column filter: "edited by" is a filter the backend offers on the entity
 * (ModifiedByFilterSupport), and the names come from the history, not from a field of the entity.
 */
export function editorsColumn<
  Row extends RowWithEditors,
>(): ComputedColumn<Row> {
  return {
    id: "editors",
    labelKey: "history.filter.modifiedBy",
    accessor: (row) => row.editors?.join(", ") ?? null,
    size: 220,
    wrap: true,
    sortable: false,
    filterKind: null,
    cell: ({ row }) => <EditorPills editors={row.original.editors} />,
  };
}

function EditorPills({ editors }: { editors?: string[] | null }) {
  if (!editors?.length) return null;
  return (
    <div className="flex flex-wrap gap-1">
      {editors.map((name) => (
        <Badge key={name} variant="secondary">
          {name}
        </Badge>
      ))}
    </div>
  );
}
