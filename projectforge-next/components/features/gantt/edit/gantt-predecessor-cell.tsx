"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { MinusSignIcon, TaskDone01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { TaskSelectModal } from "@/components/shared/tasks/task-select-modal";
import { findNode, type GanttRow } from "../gantt-tree";
import { GanttTaskValueCell } from "./gantt-task-value-cell";
import { useGanttEditorContext } from "./use-gantt-editor";

/**
 * The predecessor: its title, a structure element picker and an unselect button. A predecessor in this
 * chart shows its (possibly edited) title from the tree, one outside it the title the backend sent.
 */
export function GanttPredecessorCell({ row }: { row: GanttRow }) {
  const t = useTranslations();
  const editor = useGanttEditorContext();
  const [picking, setPicking] = useState(false);
  const { node } = row;
  const id = node.predecessorId ?? null;
  const title =
    id == null
      ? ""
      : (findNode(editor.root, id)?.title ?? node.predecessorTitle ?? "");
  return (
    <GanttTaskValueCell
      node={node}
      field="PREDECESSOR"
      taskValue={node.task?.predecessorTitle ?? ""}
    >
      <div className="flex min-w-0 items-center gap-0.5">
        <span className="min-w-0 flex-1 truncate">{title}</span>
        <HintTooltip text={t("tooltip.selectTask")}>
          <Button
            type="button"
            variant="ghost"
            size="icon-xs"
            aria-label={t("tooltip.selectTask")}
            onClick={() => setPicking(true)}
          >
            <HugeiconsIcon icon={TaskDone01Icon} size={14} />
          </Button>
        </HintTooltip>
        {id != null && (
          <HintTooltip text={t("tooltip.unselectTask")}>
            <Button
              type="button"
              variant="ghost"
              size="icon-xs"
              aria-label={t("tooltip.unselectTask")}
              onClick={() =>
                editor.update(node.id, {
                  predecessorId: null,
                  predecessorTitle: null,
                })
              }
            >
              <HugeiconsIcon icon={MinusSignIcon} size={14} />
            </Button>
          </HintTooltip>
        )}
      </div>
      {picking && (
        <TaskSelectModal
          value={id != null && id > 0 ? id : node.id > 0 ? node.id : null}
          open
          onOpenChange={setPicking}
          onChange={(task) =>
            editor.update(node.id, {
              predecessorId: task?.id ?? null,
              predecessorTitle: task?.title ?? null,
            })
          }
        />
      )}
    </GanttTaskValueCell>
  );
}
