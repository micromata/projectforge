"use client";

import { useState } from "react";
import { TaskSelectControl } from "@/components/shared/tasks/task-select-control";
import { TaskSelectModal } from "@/components/shared/tasks/task-select-modal";

/**
 * The value of a mass-update field pointing at a task: the path with type-ahead and the structure tree in a
 * dialog, as the edit forms pick a task (see TaskKost2Picker). Id in, id out — the parameter carries the id
 * only (`MassUpdateParameter.id`), the path is fetched by [TaskSelectControl].
 */
export function MassUpdateTaskControl({
  id,
  label,
  onChange,
}: {
  id: number | null;
  label: string;
  onChange: (id: number | undefined) => void;
}) {
  const [open, setOpen] = useState(false);
  // Where the tree opens rooted after a drill-down click on an ancestor; null roots it at the selection.
  const [rootAtId, setRootAtId] = useState<number | null>(null);
  const change = (task: { id: number } | null) =>
    onChange(task?.id ?? undefined);
  return (
    <>
      <TaskSelectControl
        taskId={id}
        ariaLabel={label}
        showEditLink={false}
        onOpen={() => {
          setRootAtId(null);
          setOpen(true);
        }}
        onSelect={change}
        openTreeOnAncestorClick
        onDrillDown={(task) => {
          setRootAtId(task.id);
          setOpen(true);
        }}
      />
      <TaskSelectModal
        value={id}
        rootTaskId={rootAtId}
        onChange={change}
        open={open}
        onOpenChange={(next) => {
          setOpen(next);
          if (!next) setRootAtId(null);
        }}
      />
    </>
  );
}
