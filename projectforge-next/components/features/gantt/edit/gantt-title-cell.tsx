"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowDown01Icon, ArrowRight01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import type { GanttRow } from "../gantt-tree";
import { GanttRowMenu } from "./gantt-row-menu";
import { GanttTaskValueCell } from "./gantt-task-value-cell";
import { useGanttEditorContext } from "./use-gantt-editor";

/**
 * The title: indented by depth, with the open/close toggle and the context menu in front. A Gantt-only
 * object is set in italics (Wicket wraps it in asterisks), the marked one in bold red.
 */
export function GanttTitleCell({ row }: { row: GanttRow }) {
  const t = useTranslations();
  const editor = useGanttEditorContext();
  const { node, depth, hasChildren, open } = row;
  const title = node.title ?? "";
  return (
    <div
      className="flex min-w-0 items-center gap-0.5"
      style={{ paddingLeft: `${depth * 0.9}rem` }}
    >
      {hasChildren ? (
        <Button
          type="button"
          variant="ghost"
          size="icon-xs"
          aria-label={`${t(open ? "collapse" : "expand")}: ${title}`}
          aria-expanded={open}
          onClick={() => editor.setOpen(node.id, !open)}
        >
          <HugeiconsIcon
            icon={open ? ArrowDown01Icon : ArrowRight01Icon}
            size={14}
          />
        </Button>
      ) : (
        <span className="size-6 shrink-0" />
      )}
      <GanttRowMenu row={row} title={title} />
      <div className="min-w-0 flex-1">
        <GanttTaskValueCell
          node={node}
          field="TITLE"
          taskValue={node.task?.title ?? ""}
        >
          <Input
            value={title}
            onChange={(e) => editor.update(node.id, { title: e.target.value })}
            aria-label={t("title")}
            className={cn(
              "h-7",
              node.id < 0 && "italic",
              editor.marked === node.id && "font-bold text-destructive"
            )}
          />
        </GanttTaskValueCell>
      </div>
    </div>
  );
}
