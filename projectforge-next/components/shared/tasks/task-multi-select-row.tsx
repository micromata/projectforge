"use client";

import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Cancel01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
import { fetchTaskInfo, type TaskNode } from "@/lib/rs/task";
import { TaskPath } from "./task-path";

interface TaskMultiSelectRowProps {
  entry: EntityRef;
  disabled?: boolean;
  /** An ancestor was picked from the path: it takes the entry's place. */
  onReplace: (task: TaskNode) => void;
  onRemove: () => void;
}

/**
 * One picked task of [TaskMultiSelectField]: its path, where an ancestor click replaces the entry by that
 * parent element, and a button removing it. The home button of the path removes it as well — in a single
 * select it clears the selection, which is what removing is here.
 */
export function TaskMultiSelectRow({
  entry,
  disabled,
  onReplace,
  onRemove,
}: TaskMultiSelectRowProps) {
  const t = useTranslations();
  const { data: task } = useQuery({
    queryKey: ["taskInfo", entry.id],
    queryFn: ({ signal }) => fetchTaskInfo(entry.id, signal),
    staleTime: Infinity,
  });
  // Until the answer is in, the entry's own label stands in for the path.
  const node = task ?? { id: entry.id, title: entry.displayName };

  return (
    <li className="flex min-w-0 items-center gap-2">
      <div className="min-w-0 shrink">
        <TaskPath
          task={node}
          onSelect={(picked) => (picked ? onReplace(picked) : onRemove())}
          disabled={disabled}
        />
      </div>
      <Button
        type="button"
        variant="ghost"
        size="icon"
        disabled={disabled}
        aria-label={`${t("delete")}: ${node.title ?? entry.displayName}`}
        onClick={onRemove}
        className="size-6 shrink-0"
      >
        <HugeiconsIcon icon={Cancel01Icon} size={12} />
      </Button>
    </li>
  );
}
