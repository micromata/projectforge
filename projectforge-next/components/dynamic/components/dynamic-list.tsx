"use client";

import { useMemo } from "react";
import type { DynamicComponentProps } from "../dynamic-renderer";
import { DynamicRenderer } from "../dynamic-renderer";
import { DynamicLayoutContext, useDynamicLayout } from "../dynamic-context";
import { getByPath, type DataObject } from "@/lib/dynamic/path";
import type { DynamicLayoutNode } from "@/lib/rs/types";

interface ListEntry extends DataObject {
  number?: number;
}

/**
 * Repeats a layout for every entry of a list in the data (org.projectforge.ui.UIList), e.g. the
 * positions of an order.
 *
 * The child elements address their fields as `<elementVar>.<field>`, so each entry gets a context
 * whose data is `{ [elementVar]: entry }` and whose `setData` writes back into the list. The list
 * itself keeps its order via the entries' `number`.
 */
export function DynamicList({ node }: DynamicComponentProps) {
  const context = useDynamicLayout();
  const { data, setData, translate } = context;

  const listId = node.listId as string;
  const elementVar = node.elementVar as string;
  const positionLabel = node.positionLabel as string | undefined;
  const content = node.content as DynamicLayoutNode[] | undefined;

  const list = useMemo(() => {
    const raw = getByPath(data, listId);
    const entries = Array.isArray(raw) ? (raw as ListEntry[]) : [];
    return [...entries].sort((a, b) => (a.number ?? 0) - (b.number ?? 0));
  }, [data, listId]);

  return (
    <div className="flex flex-col gap-4">
      {list.map((entry, index) => (
        <DynamicLayoutContext.Provider
          key={entry.number ?? index}
          value={{
            ...context,
            data: { [elementVar]: entry },
            setData: (patch) => {
              // The patch keys are prefixed with the element var; strip it before merging.
              const entryPatch = Object.fromEntries(
                Object.entries(patch).map(([path, value]) => [
                  path.startsWith(`${elementVar}.`)
                    ? path.slice(elementVar.length + 1)
                    : path,
                  value,
                ])
              );
              setData({
                [listId]: list.map((it) =>
                  it === entry ? { ...it, ...entryPatch } : it
                ),
              });
            },
          }}
        >
          <fieldset className="rounded border p-3">
            <legend className="px-1 text-sm text-muted-foreground">
              {`${translate(positionLabel ?? "label.position.short")} #${entry.number ?? index + 1}`}
            </legend>
            <div className="flex flex-col gap-4">
              <DynamicRenderer content={content} />
            </div>
          </fieldset>
        </DynamicLayoutContext.Provider>
      ))}
    </div>
  );
}
