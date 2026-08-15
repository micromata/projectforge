"use client";

import { useQuery } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import { HierarchyIcon } from "@hugeicons/core-free-icons";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { fetchTaskInfo } from "@/lib/rs/task";

interface TaskChipProps {
  taskId: number;
  displayName: string;
}

/**
 * Compact task reference for summary rows: a hierarchy icon + leaf name, with a tooltip that shows
 * the full ancestor path on hover.
 */
export function TaskChip({ taskId, displayName }: TaskChipProps) {
  const { data: task } = useQuery({
    queryKey: ["taskInfo", taskId],
    queryFn: ({ signal }) => fetchTaskInfo(taskId, signal),
    staleTime: Infinity,
  });

  const title = task?.title ?? displayName;
  const ancestors = task?.path ?? [];
  const pathLabel =
    ancestors.length > 0
      ? [...ancestors.map((a) => a.title), title].join(" / ")
      : null;

  return (
    <HintTooltip plain text={pathLabel}>
      <span className="flex items-center gap-1">
        <HugeiconsIcon
          icon={HierarchyIcon}
          size={12}
          className="shrink-0 text-muted-foreground"
        />
        <span>{title}</span>
      </span>
    </HintTooltip>
  );
}
