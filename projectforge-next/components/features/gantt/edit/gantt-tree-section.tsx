"use client";

import { useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Add01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { DataTable } from "@/components/data-table";
import { fetchGanttObjects } from "@/lib/rs/gantt";
import { addChild, nextId, type GanttRow } from "../gantt-tree";
import { useGanttColumns } from "./use-gantt-columns";
import { GanttEditorProvider, useGanttEditor } from "./use-gantt-editor";
import { useGanttPreview } from "./use-gantt-preview";

const NO_DATES = {};

/**
 * The editable Gantt tree: one row per object below the chart's task. Choosing another task replaces the
 * tree by that task's structure, as Wicket's form does on a task change.
 */
export function GanttTreeSection() {
  const t = useTranslations();
  const preview = useGanttPreview();
  const editor = useGanttEditor(preview.data?.dates ?? NO_DATES);
  const columns = useGanttColumns();
  const { root, taskId, setRoot } = editor;

  const stale = taskId != null && root?.id !== taskId;
  const reload = useQuery({
    queryKey: ["ganttObjects", taskId],
    queryFn: ({ signal }) => fetchGanttObjects(taskId!, signal),
    enabled: stale,
  });
  useEffect(() => {
    if (stale && reload.data) setRoot(reload.data);
  }, [stale, reload.data, setRoot]);

  const addActivity = () => {
    if (!root) return;
    setRoot(
      addChild(root, root.id, {
        id: nextId(root),
        visible: true,
        title: t("untitled"),
      })
    );
  };

  return (
    <GanttEditorProvider value={editor}>
      <div className="flex flex-col gap-2">
        {/* Bounded and scrolling inside, as every table inside a form (see WizardPreview). */}
        <div className="flex max-h-[32rem] flex-col">
          <DataTable<GanttRow>
            columns={columns}
            data={editor.rows}
            isLoading={stale && reload.isLoading}
            enableColumnFilters={false}
            manualSorting={false}
            manualPagination
            showPagination={false}
            dense
            getRowId={(row) => String(row.node.id)}
            emptyState={t("datatable.no-records-found")}
          />
        </div>
        <div>
          <Button
            type="button"
            variant="outline"
            size="sm"
            disabled={!root}
            onClick={addActivity}
          >
            <HugeiconsIcon icon={Add01Icon} size={14} />
            {t("gantt.action.newActivity")}
          </Button>
        </div>
      </div>
    </GanttEditorProvider>
  );
}
