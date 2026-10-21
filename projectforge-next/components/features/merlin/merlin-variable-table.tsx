"use client";

import { useMemo } from "react";
import type { ColumnDef } from "@tanstack/react-table";
import { useTranslations } from "next-intl";
import { DataTable } from "@/components/data-table";
import { StatusPill } from "@/components/shared/status-pill";
import type { MerlinVariable } from "@/lib/rs/merlin";
import { variableTone } from "./use-merlin-variables";
import { MERLIN_VARIABLE_TYPE_KEYS } from "./merlin-variable-types";

type Column = ColumnDef<MerlinVariable, unknown>;

function column(
  id: string,
  label: string,
  size: number,
  value: (row: MerlinVariable) => React.ReactNode
): Column {
  return {
    id,
    header: label,
    size,
    enableSorting: false,
    meta: { label, wrap: true },
    cell: ({ row }) => value(row.original),
  };
}

/**
 * The input or the dependent variables of a template, as the legacy page listed them; the name is marked by
 * the variable's role (see variableTone). A row opens the variable dialog.
 */
export function MerlinVariableTable({
  rows,
  dependent,
  onOpen,
}: {
  rows: readonly MerlinVariable[];
  dependent: boolean;
  onOpen?: (variable: MerlinVariable) => void;
}) {
  const t = useTranslations();
  const yesNo = (value: boolean | null | undefined) => (value ? t("yes") : "");
  const columns = useMemo<Column[]>(() => {
    const name = column(
      "name",
      t("plugins.merlin.variable.name"),
      180,
      (row) => <StatusPill tone={variableTone(row)} label={row.name} />
    );
    if (dependent) {
      return [
        name,
        column("used", t("plugins.merlin.variable.used"), 80, (row) =>
          yesNo(row.used)
        ),
        column(
          "dependsOn",
          t("plugins.merlin.variable.dependsOn"),
          140,
          (row) => row.dependsOnName
        ),
        column(
          "masterValues",
          t("plugins.merlin.variable.master.values"),
          220,
          (row) => row.mappingMasterValues
        ),
        column(
          "mapping",
          t("plugins.merlin.variable.mapping._"),
          260,
          (row) => row.mappingValues
        ),
      ];
    }
    return [
      name,
      column(
        "sortName",
        t("plugins.merlin.variable.sortName._"),
        120,
        (row) => row.sortName
      ),
      column(
        "type",
        t("plugins.merlin.variable.type._"),
        110,
        (row) => row.type && t(MERLIN_VARIABLE_TYPE_KEYS[row.type])
      ),
      column("required", t("plugins.merlin.variable.required"), 90, (row) =>
        yesNo(row.required)
      ),
      column("unique", t("plugins.merlin.variable.unique"), 90, (row) =>
        yesNo(row.unique)
      ),
      column("master", t("plugins.merlin.variable.master._"), 110, (row) =>
        yesNo(row.masterVariable)
      ),
      column(
        "allowedValues",
        t("plugins.merlin.variable.allowedValues._"),
        220,
        (row) => row.allowedValues?.join(", ")
      ),
      column("description", t("description"), 260, (row) => row.description),
    ];
    // yesNo only depends on t.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [t, dependent]);
  return (
    <DataTable
      columns={columns}
      data={rows as MerlinVariable[]}
      enableColumnFilters={false}
      manualSorting={false}
      showPagination={false}
      dense
      autoHeight
      enableColumnResizing
      getRowId={(row) => String(row.id ?? row.name)}
      onRowClick={onOpen}
    />
  );
}
