"use client";

import {
  useEntityData,
  useFormReadOnly,
} from "@/components/shared/form/form-context";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { TaskSelectField } from "@/components/shared/tasks/task-select-field";
import { TASK_METADATA } from "@/lib/metadata/task.generated";
import type { TaskDetail } from "../types";

/**
 * The parent task, as Wicket's TaskEditForm had it: mandatory, and left out for the root task, which is
 * the one task without a parent and can't be given one (`TaskDao` refuses both a missing parent and a
 * parent of the root). The root is recognised by the task the form was filled from: stored, and without
 * a parent.
 */
export function ParentTaskField({ className }: { className?: string }) {
  const label = useFieldLabels(TASK_METADATA);
  const task = useEntityData<TaskDetail>();
  const readOnly = useFormReadOnly();
  if (task?.id != null && task.parentTask == null) return null;
  return (
    <TaskSelectField
      name="parentTask"
      label={label("parentTask")}
      className={className}
      disabled={readOnly}
      required
    />
  );
}
