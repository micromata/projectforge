"use client";

import type { ColumnDef } from "@tanstack/react-table";
import type { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";

type T = ReturnType<typeof useTranslations>;

/** Which rows of a log table have their context opened (DataTable.renderRowDetail). */
export interface ContextToggle {
  isOpen: (id: string) => boolean;
  toggle: (id: string) => void;
}

/** The column with the button opening the context of an entry below it. */
export function contextColumn<E>(
  t: T,
  context: ContextToggle
): ColumnDef<E, unknown> {
  const label = t("system.admin.adminLogViewer.logFile.context");
  return {
    id: "context",
    header: label,
    size: 90,
    enableSorting: false,
    meta: { label },
    cell: ({ row }) => {
      const open = context.isOpen(row.id);
      return (
        <Button
          size="xs"
          variant={open ? "secondary" : "outline"}
          aria-expanded={open}
          onClick={() => context.toggle(row.id)}
        >
          {label}
        </Button>
      );
    },
  };
}

/** The columns with [column] before the stack trace, whose opened text would push it out of sight. */
export function beforeStackTrace<E>(
  columns: ColumnDef<E, unknown>[],
  column: ColumnDef<E, unknown>
): ColumnDef<E, unknown>[] {
  const index = columns.findIndex((it) => it.id === "stackTrace");
  const result = [...columns];
  result.splice(index < 0 ? result.length : index, 0, column);
  return result;
}
