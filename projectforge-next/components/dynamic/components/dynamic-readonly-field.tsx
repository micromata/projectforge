"use client";

import type { DynamicComponentProps } from "../dynamic-renderer";
import { useDynamicLayout } from "../dynamic-context";
import { getByPath } from "@/lib/dynamic/path";
import { Label } from "@/components/ui/label";

export function DynamicReadonlyField({ node }: DynamicComponentProps) {
  const { data, translate } = useDynamicLayout();

  const id = node.id as string;
  const label = node.label as string | undefined;

  const rawValue = getByPath(data, id);
  const displayValue = rawValue != null ? String(rawValue) : "—";

  return (
    <div className="flex flex-col gap-1 flex-1 min-w-0">
      {label && (
        <Label className="text-sm text-muted-foreground">
          {translate(label)}
        </Label>
      )}
      <span className="text-sm">{displayValue}</span>
    </div>
  );
}
