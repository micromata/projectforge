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
        <div className="max-h-[65vh] overflow-auto rounded-md border p-2">
          <TaskTreePanel
            highlightTaskId={value}
            onSelect={handleSelect}
            selectMode
          />
        </div>
      </DialogContent>
    </Dialog>
  );
}
