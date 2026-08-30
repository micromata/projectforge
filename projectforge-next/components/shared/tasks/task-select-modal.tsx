"use client";

import { useTranslations } from "next-intl";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { TaskTreePanel } from "./task-tree-panel";
import type { TaskNode } from "@/lib/rs/task";

export interface TaskSelectModalProps {
  value: number | null;
  onChange: (task: TaskNode | null) => void;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function TaskSelectModal({
  value,
  onChange,
  open,
  onOpenChange,
}: TaskSelectModalProps) {
  const t = useTranslations();

  const handleSelect = (task: TaskNode) => {
    onChange(task);
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="w-[95vw] !max-w-[95vw]">
        <DialogHeader>
          <DialogTitle>{t("task.tree.title.select")}</DialogTitle>
        </DialogHeader>
        {/* A bounded flex column, not a scrolling block: the panel is built to scroll *inside* its own
            table (flex-1 + min-h-0, like the list page's `flex flex-1 overflow-hidden` host), and that
            inner scroller is the one the highlight jumps to (see useHighlightedRow). A wrapper that
            scrolled itself would leave the table's scroller idle, so the current task never came into
            view. */}
        <div className="flex max-h-[65vh] flex-col overflow-hidden rounded-md border p-2">
          <TaskTreePanel
            highlightTaskId={value}
            onSelect={handleSelect}
            selectMode
            rootNavigable
          />
        </div>
      </DialogContent>
    </Dialog>
  );
}
