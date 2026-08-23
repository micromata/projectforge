"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import type { ColumnDef } from "@tanstack/react-table";
import { Badge } from "@/components/ui/badge";
import { DataTable } from "@/components/data-table";
import { SectionCard } from "@/components/shared/section-card";
import {
  AccessOperationsHeader,
  AccessRightsMatrix,
  AccessTypeLegend,
} from "@/components/shared/access/access-rights-matrix";
import type { TaskWizardResult } from "@/lib/rs/task";
import { STATUS_KEYS, STATUS_VARIANTS } from "./result-model";
import { buildPreview, type WizardPreviewRow } from "./preview-model";

export interface WizardPreviewProps {
  preview: TaskWizardResult;
  isFetching?: boolean;
}

/**
 * What the wizard would do, while the element and the groups are still being picked: one row per element
 * of the path, one column per group, and in every cell the rights that group would get on that element -
 * the matrix of the Wicket access panel (see AccessRightsMatrix), plus what that means for the entry that
 * is there today (new, changed, already correct).
 *
 * Answered by the backend rather than worked out here (`/rs/taskWizard/preview`), so the preview cannot
 * say anything else than the write does.
 */
export function WizardPreview({ preview, isFetching }: WizardPreviewProps) {
  const t = useTranslations();
  const { columns, rows } = useMemo(() => buildPreview(preview), [preview]);

  const columnDefs = useMemo<ColumnDef<WizardPreviewRow, unknown>[]>(() => {
    const elementColumn: ColumnDef<WizardPreviewRow, unknown> = {
      id: "element",
      header: t("task.wizard.preview.element"),
      size: 260,
      enableSorting: false,
      meta: { label: t("task.wizard.preview.element"), wrap: true },
      cell: ({ row }) => (
        <div
          className="flex h-4 items-center gap-2"
          // The path is one chain, so the depth in it is the row's position - the indentation of the
          // structure tree's own tree cell (see TreeCell).
          style={{ paddingLeft: `${row.original.indent * 0.9}rem` }}
        >
          <span className="truncate font-medium">
            {row.original.taskTitle ?? ""}
          </span>
          {row.original.pickedElement && (
            <span className="shrink-0 text-muted-foreground">
              ({t("task.wizard.preview.picked")},{" "}
              {t("task.wizard.preview.recursive")})
            </span>
          )}
        </div>
      ),
    };
    const legendColumn: ColumnDef<WizardPreviewRow, unknown> = {
      id: "accessType",
      // `access.type` carries a text of its own and children (see the generated catalog), so the text
      // is the `_` of the namespace.
      header: t("access.type._"),
      size: 150,
      enableSorting: false,
      meta: { label: t("access.type._"), wrap: true },
      cell: ({ row }) => {
        const rights = rightsOf(row.original);
        return rights ? <AccessTypeLegend rights={rights} t={t} /> : null;
      },
    };
    const groupColumns = columns.map<ColumnDef<WizardPreviewRow, unknown>>(
      (column) => ({
        id: column.groupType,
        size: 128,
        enableSorting: false,
        meta: {
          label: `${t(column.roleKey)} ${column.groupName ?? ""}`,
          wrap: true,
        },
        header: () => (
          <div className="flex flex-col py-1">
            <span className="truncate">
              {t(column.roleKey)}
              {column.groupName ? `: ${column.groupName}` : ""}
            </span>
            <AccessOperationsHeader t={t} />
          </div>
        ),
        cell: ({ row }) => {
          const entry = row.original.cells[column.groupType];
          if (!entry) return null;
          return (
            <div className="flex flex-col gap-1">
              <AccessRightsMatrix rights={entry.rights} t={t} />
              <Badge variant={STATUS_VARIANTS[entry.status]} className="w-fit">
                {t(STATUS_KEYS[entry.status])}
              </Badge>
            </div>
          );
        },
      })
    );
    return [elementColumn, legendColumn, ...groupColumns];
  }, [columns, t]);

  return (
    <SectionCard className="flex flex-col gap-3">
      <h2 className="text-sm font-semibold">
        {t("task.wizard.preview.title")}
      </h2>
      <p className="text-sm text-muted-foreground">
        {t("task.wizard.preview.intro")}
      </p>
      {/* Bounded and scrolling inside, as every table that sits inside a form does (see
          SelectedEntriesTable): a deep path must not push the buttons below it off the screen. */}
      <div className="flex max-h-96 flex-col">
        <DataTable<WizardPreviewRow>
          columns={columnDefs}
          data={rows}
          isFetching={isFetching}
          enableColumnFilters={false}
          manualSorting={false}
          showPagination={false}
          getRowId={(row) => String(row.taskId)}
        />
      </div>
    </SectionCard>
  );
}

/**
 * The access types of a row, taken from whichever group has an entry on it: the four types are the same
 * for every group, only their permissions differ.
 */
function rightsOf(row: WizardPreviewRow) {
  return Object.values(row.cells).find((entry) => entry)?.rights;
}
