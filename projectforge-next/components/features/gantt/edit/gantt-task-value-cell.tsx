"use client";

import type { ReactNode } from "react";
import { useMutation } from "@tanstack/react-query";
import { toast } from "@/lib/toast";
import { saveGanttValueToTask, type GanttTaskField } from "@/lib/rs/gantt";
import type { GanttObject } from "../types";
import { isModified, rejectPatch } from "./gantt-task-fields";
import { GanttValueCell } from "./gantt-value-cell";
import { useGanttEditorContext } from "./use-gantt-editor";

/**
 * A value of a Gantt row bound to its structure element: shows reject/save once they differ. Saving writes
 * the value to the task at once (as Wicket does, outside the chart's own save) and takes the task's
 * answer as the new comparison base.
 */
export function GanttTaskValueCell({
  node,
  field,
  taskValue,
  children,
}: {
  node: GanttObject;
  field: GanttTaskField;
  /** The structure element's value, formatted for the reject tooltip. */
  taskValue: string;
  children: ReactNode;
}) {
  const editor = useGanttEditorContext();
  const save = useMutation({
    mutationFn: () => saveGanttValueToTask(node, field),
    onSuccess: (task) => editor.update(node.id, { task }),
    onError: (error: unknown) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });
  return (
    <GanttValueCell
      modified={isModified(node, field)}
      taskValue={taskValue}
      canSave={node.task?.updateAccess === true}
      saving={save.isPending}
      onReject={() => editor.update(node.id, rejectPatch(node, field))}
      onSave={() => save.mutate()}
    >
      {children}
    </GanttValueCell>
  );
}
