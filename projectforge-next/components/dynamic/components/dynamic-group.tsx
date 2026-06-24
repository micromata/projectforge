"use client";

import { DynamicRenderer } from "../dynamic-renderer";
import type { DynamicComponentProps } from "../dynamic-renderer";
import type { DynamicLayoutNode } from "@/lib/rs/types";
import { cn } from "@/lib/utils";

export function DynamicGroup({ node }: DynamicComponentProps) {
  const content = node.content as DynamicLayoutNode[] | undefined;
  const type = node.type as string;

  if (type === "FRAGMENT") {
    return <DynamicRenderer content={content} />;
  }

  const className = cn(
    type === "ROW" && "flex flex-wrap gap-4",
    type === "COL" && "flex-1 min-w-0",
    type === "GROUP" && "flex flex-wrap gap-4 items-start",
    type === "INLINE_GROUP" && "flex items-center gap-2"
  );

  return (
    <div className={className}>
      <DynamicRenderer content={content} />
    </div>
  );
}
