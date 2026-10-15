"use client";

import type { DynamicComponentProps } from "../dynamic-renderer";

/**
 * Placeholder for an element type the renderer does not know yet.
 *
 * Visible in dev so a gap shows up immediately when comparing a page against the legacy renderer,
 * invisible in production so an unmigrated element never breaks a page.
 */
export function DynamicFallback({ node }: DynamicComponentProps) {
  if (process.env.NODE_ENV === "development") {
    // The data type distinguishes the many flavours of INPUT, so it belongs in the label.
    const what = node.dataType ? `${node.type}/${node.dataType}` : node.type;
    return (
      <div className="rounded border border-dashed border-yellow-400 bg-yellow-50 px-2 py-1 text-xs text-yellow-700 dark:border-yellow-600 dark:bg-yellow-950 dark:text-yellow-300">
        [{String(what)}] {(node.id as string) ?? ""} not implemented
      </div>
    );
  }
  return null;
}
