"use client";

import type { DynamicComponentProps } from "../dynamic-renderer";

export function DynamicFallback({ node }: DynamicComponentProps) {
  if (process.env.NODE_ENV === "development") {
    return (
      <div className="rounded border border-dashed border-yellow-400 bg-yellow-50 px-2 py-1 text-xs text-yellow-700 dark:border-yellow-600 dark:bg-yellow-950 dark:text-yellow-300">
        [{node.type}] not implemented
      </div>
    );
  }
  return null;
}
