"use client";

import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
} from "react";
import { useStore } from "@tanstack/react-form";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import type { GanttPreview } from "@/lib/rs/gantt";
import { flattenRows, updateNode, type GanttRow } from "../gantt-tree";
import type { GanttValues } from "../gantt-schema";
import type { GanttObject } from "../types";

/** What the cells and menus of the tree table share: the tree, its open nodes and the marked node. */
export interface GanttEditor {
  root: GanttObject | null;
  rows: GanttRow[];
  /** The chart's task, the parent of "move to top". */
  taskId: number | null;
  setRoot: (next: GanttObject) => void;
  update: (id: number, patch: Partial<GanttObject>) => void;
  isOpen: (id: number) => boolean;
  setOpen: (id: number, open: boolean) => void;
  /** The node marked as predecessor / to move ("mark" in the context menu). */
  marked: number | null;
  setMarked: (id: number | null) => void;
  /** The dates the chart calculated, by node id (from the last preview). */
  calculated: GanttPreview["dates"];
}

const Ctx = createContext<GanttEditor | null>(null);

export const GanttEditorProvider = Ctx.Provider;

export function useGanttEditorContext(): GanttEditor {
  const editor = useContext(Ctx);
  if (!editor) throw new Error("No GanttEditorProvider above.");
  return editor;
}

interface FormState {
  values: GanttValues;
}

/** Builds the editor on the form's `root` and `openNodes` values. */
export function useGanttEditor(calculated: GanttPreview["dates"]): GanttEditor {
  const form = useEntityEditForm();
  const root = useStore(
    form.store,
    (s: unknown) => (s as FormState).values.root
  );
  const openNodes = useStore(
    form.store,
    (s: unknown) => (s as FormState).values.openNodes
  );
  const onlyVisibles = useStore(
    form.store,
    (s: unknown) => (s as FormState).values.showOnlyVisibles
  );
  const taskId = useStore(
    form.store,
    (s: unknown) => (s as FormState).values.task?.id ?? null
  );
  const [marked, setMarked] = useState<number | null>(null);

  const open = useMemo(() => new Set(openNodes ?? []), [openNodes]);
  const rows = useMemo(
    () => flattenRows(root, open, onlyVisibles),
    [root, open, onlyVisibles]
  );

  const setRoot = useCallback(
    (next: GanttObject) => form.setFieldValue("root", next),
    [form]
  );
  const update = useCallback(
    (id: number, patch: Partial<GanttObject>) => {
      const current = (form.state as FormState).values.root;
      if (current) form.setFieldValue("root", updateNode(current, id, patch));
    },
    [form]
  );
  const setOpen = useCallback(
    (id: number, value: boolean) => {
      const current = new Set<number>(
        (form.state as FormState).values.openNodes ?? []
      );
      if (value) current.add(id);
      else current.delete(id);
      // Saved with the chart, as Wicket stores the open nodes — but browsing the tree is no change the
      // unsaved-changes warning should be about.
      form.setFieldValue("openNodes", [...current], { dontUpdateMeta: true });
    },
    [form]
  );

  return useMemo(
    () => ({
      root,
      rows,
      taskId,
      setRoot,
      update,
      isOpen: (id: number) => open.has(id),
      setOpen,
      marked,
      setMarked,
      calculated,
    }),
    [root, rows, taskId, setRoot, update, open, setOpen, marked, calculated]
  );
}
