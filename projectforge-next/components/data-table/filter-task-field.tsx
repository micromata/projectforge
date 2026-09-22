"use client";

import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Label } from "@/components/ui/label";
import { TaskSelectControl } from "@/components/shared/tasks/task-select-control";
import { TaskSelectModal } from "@/components/shared/tasks/task-select-modal";
import { fetchTaskInfo, type TaskNode } from "@/lib/rs/task";
import type { FilterInputProps } from "./filter-field-inputs";

/**
 * The task variant of an OBJECT filter (AutoCompletion.Type.TASK): the structure tree, not a plain
 * type-ahead. A task title only means something in its place in the structure, so the value is picked
 * through the breadcrumb path, a type-ahead, and the whole tree — the latter in a wide modal dialog
 * ([TaskSelectModal]), since the filter popover is far too narrow for it.
 *
 * The stored value carries `id` — what `MagicFilterProcessor` reads — plus `displayName`, the task's
 * path, so the pill and a restored favorite can name it without a lookup (the plain autocomplete gets
 * that string from the server; here the tree hands back only a node, so we resolve the path ourselves).
 */
export function FilterTaskField({
  value,
  onChange,
  label,
  id,
}: FilterInputProps) {
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);

  const change = async (task: { id: number } | null) => {
    if (task == null) {
      onChange(undefined);
      return;
    }
    // A cache hit when the task came with no path: [TaskSelectControl] fetches the same task under the
    // same key for its breadcrumb, and so does the modal's tree once a node is picked.
    const info = await queryClient.fetchQuery({
      queryKey: ["taskInfo", task.id],
      queryFn: ({ signal }) => fetchTaskInfo(task.id, signal),
      staleTime: Infinity,
    });
    onChange({ id: task.id, displayName: taskPathLabel(info) });
  };

  return (
    <div className="space-y-1">
      <Label htmlFor={`filter-${id}`} className="text-xs">
        {label}
      </Label>
      <TaskSelectControl
        taskId={value?.id ?? null}
        ariaLabel={label}
        showEditLink={false}
        onOpen={() => setOpen(true)}
        onSelect={change}
      />
      <TaskSelectModal
        value={value?.id ?? null}
        onChange={change}
        open={open}
        onOpenChange={setOpen}
      />
    </div>
  );
}

/**
 * A task as its path, `"Ancestor | … | Task"` — the same " | "-joined form the backend's autosearch
 * label uses, so the pill reads the same whether the value was just picked or restored from a favorite.
 *
 * `path` holds the ancestors root-first and excludes the task itself (TaskServicesRest.createTask).
 */
function taskPathLabel(task: TaskNode): string {
  return [...(task.path ?? []).map((node) => node.title), task.title]
    .filter(Boolean)
    .join(" | ");
}
