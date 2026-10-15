"use client";

import { useMutation } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { toast } from "@/lib/toast";
import { moveGanttTask, saveGanttObjectAsTask } from "@/lib/rs/gantt";
import {
  addChild,
  findNode,
  moveNode,
  nextId,
  removeNode,
  type GanttRow,
} from "../gantt-tree";
import { useGanttEditorContext } from "./use-gantt-editor";

const showError = (error: unknown) =>
  toast.error(error instanceof Error ? error.message : String(error));

/**
 * The actions of a row's context menu, ported from Wicket's GanttChartEditTreeTablePanel. The ones touching
 * a structure element (move, save as task) write it at once, as Wicket does; everything else changes the
 * unsaved chart only.
 */
export function useGanttRowActions(row: GanttRow) {
  const t = useTranslations();
  const editor = useGanttEditorContext();
  const { node } = row;
  const root = editor.root;
  const marked = editor.marked;
  const markedNode = marked == null || !root ? null : findNode(root, marked);
  /** "Move to top" when the marked node is this one, "move here" otherwise. */
  const toTop = marked === node.id;
  const target = toTop ? (root?.id ?? null) : node.id;

  const move = useMutation({
    mutationFn: async () => {
      if (!markedNode || target == null) return;
      if (markedNode.id > 0) {
        // The structure element moves too: below the chart's task for "to top", else below this node's.
        const parentTaskId = toTop
          ? editor.taskId
          : node.id > 0
            ? node.id
            : null;
        if (parentTaskId != null)
          await moveGanttTask(markedNode.id, parentTaskId);
      }
    },
    onSuccess: () => {
      if (!markedNode || target == null || !editor.root) return;
      editor.setRoot(moveNode(editor.root, markedNode.id, target));
      if (!toTop) editor.setOpen(node.id, true);
    },
    onError: showError,
  });

  const saveAsTask = useMutation({
    mutationFn: () => saveGanttObjectAsTask(node, row.parentId!),
    onSuccess: ({ id, task }) => {
      editor.update(node.id, { id, task });
      if (editor.isOpen(node.id)) {
        editor.setOpen(node.id, false);
        editor.setOpen(id, true);
      }
      if (editor.marked === node.id) editor.setMarked(id);
    },
    onError: showError,
  });

  return {
    canPaste: markedNode != null && marked !== node.id,
    canMove:
      markedNode != null &&
      (toTop
        ? row.parentId !== root?.id
        : findNode(markedNode, node.id) == null),
    toTop,
    /** A moved structure element needs a confirmation, a Gantt-only object does not. */
    moveNeedsConfirm: (markedNode?.id ?? 0) > 0,
    mark: () => editor.setMarked(toTop ? null : node.id),
    paste: () =>
      markedNode &&
      editor.update(node.id, {
        predecessorId: markedNode.id,
        predecessorTitle: markedNode.title ?? null,
      }),
    addSubActivity: () => {
      if (!root) return;
      const child = { id: nextId(root), visible: true, title: t("untitled") };
      editor.setRoot(addChild(root, node.id, child));
      editor.setOpen(node.id, true);
    },
    move: () => move.mutate(),
    remove: () => {
      if (root) editor.setRoot(removeNode(root, node.id));
      if (marked === node.id) editor.setMarked(null);
    },
    /** Checked before asking: a new structure element needs one as its parent. */
    canSaveAsTask: () => {
      if ((row.parentId ?? 0) > 0) return true;
      toast.error(t("gantt.error.parentObjectIsNotAPFTask"));
      return false;
    },
    saveAsTask: () => saveAsTask.mutate(),
    pending: move.isPending || saveAsTask.isPending,
  };
}
