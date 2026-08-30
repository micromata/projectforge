"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowDown01Icon, ArrowUp01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { Collapsible, CollapsibleContent } from "@/components/ui/collapsible";
import { fetchTaskInfo, type TaskNode } from "@/lib/rs/task";
import { cn } from "@/lib/utils";
import { TaskPath } from "./task-path";
import { TaskTreePanel } from "./task-tree-panel";

export interface TaskSelectProps {
  /** Id of the selected task, null while nothing is selected. */
  value: number | null;
  onChange: (id: number | null) => void;
  id?: string;
  /** Accessible name of the toggle, when no `<label htmlFor>` names it. */
  "aria-label"?: string;
  className?: string;
}

/**
 * Picks a task: its path as a breadcrumb, with the whole tree one click away.
 *
 * A tree instead of an autocomplete, because a task title („AK", „Verwaltungsteam") only means
 * something in its place in the structure — which is also why the collapsed state shows the path and
 * not just the title.
 *
 * Context-free like [EntityAutocomplete]: it takes an id and hands one back, so a hand-built form and
 * a filter row can both use it. The task itself is fetched here rather than passed in, since only the
 * backend knows the ancestors the breadcrumb is made of.
 */
export function TaskSelect({
  value,
  onChange,
  id,
  className,
  "aria-label": ariaLabel,
}: TaskSelectProps) {
  const t = useTranslations();
  const [open, setOpen] = useState(false);

  const { data: task } = useQuery({
    queryKey: ["taskInfo", value],
    queryFn: ({ signal }) => fetchTaskInfo(value!, signal),
    enabled: value != null,
    // The path of a task changes only when the tree is restructured, which is rare and never here.
    staleTime: Infinity,
  });

  const select = (selected: TaskNode | null) => {
    onChange(selected?.id ?? null);
    setOpen(false);
  };

  return (
    <Collapsible
      open={open}
      onOpenChange={setOpen}
      className={cn("min-w-0", className)}
    >
      <div className="flex min-w-0 items-center gap-2">
        <div className="min-w-0 flex-1">
          <TaskPath
            task={(value != null && task) || null}
            onSelect={select}
            onOpen={() => setOpen(true)}
          />
        </div>
        {/* Not CollapsibleTrigger: the panel is also closed by picking a task, so the open state is
            held here anyway and the trigger would only duplicate it. */}
        <Button
          id={id}
          type="button"
          variant="outline"
          size="icon"
          aria-expanded={open}
          aria-label={ariaLabel ?? t("task.tree.title.select")}
          onClick={() => setOpen(!open)}
          className="size-7 shrink-0"
        >
          <HugeiconsIcon
            icon={open ? ArrowUp01Icon : ArrowDown01Icon}
            size={14}
          />
        </Button>
      </div>
      <CollapsibleContent>
        {/* Bounded and scrollable: the tree can be hundreds of rows long, and it sits inside a form
            that must stay reachable. */}
        <div className="mt-2 flex max-h-[600px] flex-col overflow-auto rounded-md border p-2">
          <TaskTreePanel
            highlightTaskId={value}
            onSelect={select}
            selectMode
            rootNavigable
          />
        </div>
      </CollapsibleContent>
    </Collapsible>
  );
}
