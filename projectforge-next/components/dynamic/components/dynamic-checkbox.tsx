"use client";

import type { DynamicComponentProps } from "../dynamic-renderer";
import { useDynamicLayout } from "../dynamic-context";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";

export function DynamicCheckbox({ node }: DynamicComponentProps) {
  const { data, setData, translate } = useDynamicLayout();

  const id = node.id as string;
  const label = node.label as string | undefined;
  const checked = Boolean(getNestedValue(data, id));

  return (
    <div className="flex items-center gap-2">
      <Checkbox
        id={id}
        checked={checked}
        onCheckedChange={(value) => setData({ [id]: value === true })}
      />
      {label && (
        <Label htmlFor={id} className="text-sm font-normal">
          {translate(label)}
        </Label>
      )}
    </div>
  );
}

function getNestedValue(obj: Record<string, unknown>, path: string): unknown {
  return path.split(".").reduce<unknown>((acc, key) => {
    if (acc && typeof acc === "object") return (acc as Record<string, unknown>)[key];
    return undefined;
  }, obj);
}
