"use client";

import { useState } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { MoreVerticalIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { setChildrenVisible, setInvisible, type GanttRow } from "../gantt-tree";
import { useGanttEditorContext } from "./use-gantt-editor";
import { useGanttRowActions } from "./use-gantt-row-actions";

type Question = "move" | "delete" | "saveAsTask";

const QUESTIONS: Record<Question, string> = {
  move: "gantt.question.moveTask",
  delete: "question.deleteRowQuestion",
  saveAsTask: "gantt.question.saveGanttObjectAsTask",
};

/** The context menu of a Gantt row (Wicket's ContextMenu of GanttChartEditTreeTablePanel). */
export function GanttRowMenu({ row, title }: { row: GanttRow; title: string }) {
  const t = useTranslations();
  const editor = useGanttEditorContext();
  const actions = useGanttRowActions(row);
  const [question, setQuestion] = useState<Question | null>(null);
  const { node } = row;
  const ganttOnly = node.id < 0;
  const withRoot = (fn: typeof setInvisible) => () =>
    editor.root && editor.setRoot(fn(editor.root, node.id));

  const confirmed = () => {
    if (question === "move") actions.move();
    else if (question === "delete") actions.remove();
    else if (question === "saveAsTask") actions.saveAsTask();
  };

  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button
            type="button"
            variant="ghost"
            size="icon-xs"
            disabled={actions.pending}
            aria-label={`${t("label.options")}: ${title}`}
          >
            <HugeiconsIcon icon={MoreVerticalIcon} size={14} />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="start">
          <DropdownMenuItem onSelect={actions.mark}>
            {t("mark")}
          </DropdownMenuItem>
          {actions.canPaste && (
            <DropdownMenuItem onSelect={actions.paste}>
              {t("paste")} ({t("gantt.predecessor")})
            </DropdownMenuItem>
          )}
          {actions.canMove && (
            <DropdownMenuItem
              onSelect={() =>
                actions.moveNeedsConfirm ? setQuestion("move") : actions.move()
              }
            >
              {t(
                actions.toTop ? "gantt.action.moveToTop" : "gantt.action.move"
              )}
            </DropdownMenuItem>
          )}
          <DropdownMenuItem onSelect={actions.addSubActivity}>
            {t("gantt.contextMenu.newSubActivity")}
          </DropdownMenuItem>
          <DropdownMenuSeparator />
          <DropdownMenuItem onSelect={withRoot(setInvisible)}>
            {t("gantt.contextMenu.setInvisible")}
          </DropdownMenuItem>
          <DropdownMenuItem onSelect={withRoot(setChildrenVisible)}>
            {t("gantt.contextMenu.setSubTasksVisible")}
          </DropdownMenuItem>
          <DropdownMenuSeparator />
          {ganttOnly ? (
            <>
              <DropdownMenuItem
                onSelect={() =>
                  actions.canSaveAsTask() && setQuestion("saveAsTask")
                }
              >
                {t("gantt.contextMenu.saveAsTask")}
              </DropdownMenuItem>
              <DropdownMenuItem
                variant="destructive"
                onSelect={() => setQuestion("delete")}
              >
                {t("delete")}
              </DropdownMenuItem>
            </>
          ) : (
            <DropdownMenuItem asChild>
              <Link href={`/task/${node.id}`}>{t("task.title.edit")}</Link>
            </DropdownMenuItem>
          )}
        </DropdownMenuContent>
      </DropdownMenu>
      <ConfirmDialog
        open={question != null}
        onOpenChange={(open) => !open && setQuestion(null)}
        title={title}
        description={question ? t(QUESTIONS[question]) : ""}
        confirmLabel={t("yes")}
        destructive={question === "delete"}
        onConfirm={confirmed}
      />
    </>
  );
}
