"use client";

import type { DynamicComponentProps } from "../dynamic-renderer";
import { useDynamicLayout } from "../dynamic-context";

export function DynamicLabel({ node }: DynamicComponentProps) {
  const { translate } = useDynamicLayout();
  const label = (node.label as string) ?? "";

  return <span className="text-sm">{translate(label)}</span>;
}
