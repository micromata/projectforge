"use client";

import { DynamicRenderer } from "../dynamic-renderer";
import type { DynamicComponentProps } from "../dynamic-renderer";
import type { DynamicLayoutNode } from "@/lib/rs/types";
import { useDynamicLayout } from "../dynamic-context";

export function DynamicFieldset({ node }: DynamicComponentProps) {
  const { translate } = useDynamicLayout();
  const content = node.content as DynamicLayoutNode[] | undefined;
  const title = node.title as string | undefined;

  return (
    <fieldset className="rounded-lg border p-4">
      {title && (
        <legend className="px-2 text-sm font-medium text-muted-foreground">
          {translate(title)}
        </legend>
      )}
      <div className="flex flex-col gap-4">
        <DynamicRenderer content={content} />
      </div>
    </fieldset>
  );
}
