"use client";

import type { ColumnDef, Table } from "@tanstack/react-table";
import { useTranslations } from "next-intl";
import { Checkbox } from "@/components/ui/checkbox";

/** Id of the checkbox column, so a page can pin it and the stored column state can name it. */
export const SELECTION_COLUMN_ID = "__select";

/**
 * The checkbox column of a table whose rows can be picked for a mass update.
 *
 * Its own column def rather than a `ColumnDeclaration`: it shows no value of the entity, is never
 * sorted, filtered, hidden or reordered, and it is not something a page declares field by field — a
 * page switches selection on and gets this column (see EntityListPage).
 *
 * The header checkbox covers **every row the filter matched**, not the page on screen: a list holds its
 * whole result set on the client (the backend answers `getList` in one go) and pages it here, which is
 * also the set the backend registers for selection.
 *
 * A click on a cell must not travel up to the row, or the row's own click handler would replace the
 * selection the checkbox just extended.
 */
export function selectionColumn<Row>(): ColumnDef<Row> {
  return {
    id: SELECTION_COLUMN_ID,
    size: 36,
    enableSorting: false,
    enableHiding: false,
    enableResizing: false,
    enableColumnFilter: false,
    header: ({ table }) => <SelectAllCheckbox table={table} />,
    cell: ({ row }) => (
      <RowCheckbox
        checked={row.getIsSelected()}
        disabled={!row.getCanSelect()}
        onCheckedChange={(checked) => row.toggleSelected(checked)}
      />
    ),
  };
}

function SelectAllCheckbox<Row>({ table }: { table: Table<Row> }) {
  // Root namespace: both labels are bundle keys of their own (`selectAll`, `select`), not table chrome.
  const t = useTranslations();
  return (
    <RowCheckbox
      checked={
        table.getIsAllRowsSelected()
          ? true
          : table.getIsSomeRowsSelected()
            ? "indeterminate"
            : false
      }
      onCheckedChange={(checked) => table.toggleAllRowsSelected(checked)}
      label={t("selectAll")}
    />
  );
}

function RowCheckbox({
  checked,
  disabled,
  onCheckedChange,
  label,
}: {
  checked: boolean | "indeterminate";
  disabled?: boolean;
  onCheckedChange: (checked: boolean) => void;
  label?: string;
}) {
  // Root namespace: both labels are bundle keys of their own (`selectAll`, `select`), not table chrome.
  const t = useTranslations();
  return (
    <div
      className="flex items-center justify-center"
      // The row's handler would otherwise run on the same click and replace the selection.
      onClick={(event) => event.stopPropagation()}
    >
      <Checkbox
        checked={checked}
        disabled={disabled}
        onCheckedChange={(value) => onCheckedChange(value === true)}
        // `select._`: the bare key `select` has children in the bundle (`select.placeholder`), so the
        // generator nests it under the reserved "_".
        aria-label={label ?? t("select._")}
        className="size-3.5"
      />
    </div>
  );
}
