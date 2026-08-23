"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Search01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { EntitySearchList } from "@/components/shared/entity-search-list";
import type { TaskNode } from "@/lib/rs/task";

/**
 * Where the type-ahead searches: `TaskServicesRest.autosearch`, not the inherited `task/autosearch`
 * of `TaskPagesRest` (which has no search fields configured and would answer an error). It answers
 * the same `DisplayObject` as any other category, with the task's whole path as its `displayName`.
 */
const TASK_LOOKUP_URL = "task/tree/autosearch?search=:search";

export interface TaskSearchPopoverProps {
  /** Accessible name of the field this searches for, so the button says which one it belongs to. */
  ariaLabel: string;
  disabled?: boolean;
  /** A task was picked — the id and its path, which is all the search answers. */
  onSelect: (task: TaskNode) => void;
}

/**
 * Finds a task by typing part of its title or of any ancestor's — the second way into the task
 * select field, beside the tree.
 *
 * The tree answers "where does this belong", the search answers "where is the one I know the name
 * of"; in a tree of a few thousand structure elements the second question is the more common one,
 * and it is what Wicket's select panel offers next to its tree (`initAutoCompletePanels`).
 */
export function TaskSearchPopover({
  ariaLabel,
  disabled,
  onSelect,
}: TaskSearchPopoverProps) {
  const t = useTranslations();
  const [open, setOpen] = useState(false);

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          type="button"
          variant="outline"
          size="icon"
          disabled={disabled}
          aria-label={`${t("search._")} ${ariaLabel}`}
          className="size-7 shrink-0"
        >
          <HugeiconsIcon icon={Search01Icon} size={14} />
        </Button>
      </PopoverTrigger>
      {/* Wider than the trigger, which is one icon: a hit is a whole path and would be unreadable
          at the button's width. */}
      <PopoverContent align="start" className="w-96 p-0">
        <EntitySearchList
          url={TASK_LOOKUP_URL}
          active={open}
          onPick={(entry) => {
            // The path is what the hit is labelled with, and `title` is where the control expects
            // the text of the picked task (see TaskSelectControl, which then loads the task itself).
            onSelect({ id: entry.id, title: entry.displayName });
            setOpen(false);
          }}
        />
      </PopoverContent>
    </Popover>
  );
}
