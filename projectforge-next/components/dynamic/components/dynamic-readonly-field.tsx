"use client";

import type { DynamicComponentProps } from "../dynamic-renderer";
import { useDynamicLayout } from "../dynamic-context";
import { Label } from "@/components/ui/label";

export function DynamicReadonlyField({ node }: DynamicComponentProps) {
  const { data, translate } = useDynamicLayout();

  const id = node.id as string;
  const label = node.label as string | undefined;

  const rawValue = getNestedValue(data, id);
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

function getNestedValue(obj: Record<string, unknown>, path: string): unknown {
  return path.split(".").reduce<unknown>((acc, key) => {
    if (acc && typeof acc === "object") return (acc as Record<string, unknown>)[key];
    return undefined;
  }, obj);
}
