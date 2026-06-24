"use client";

import type { DynamicComponentProps } from "../dynamic-renderer";
import { useDynamicLayout } from "../dynamic-context";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";

export function DynamicTextarea({ node }: DynamicComponentProps) {
  const { data, setData, translate, validationErrors } = useDynamicLayout();

  const id = node.id as string;
  const label = node.label as string | undefined;
  const maxLength = node.maxLength as number | undefined;
  const rows = (node.rows as number) ?? 3;

  const rawValue = getNestedValue(data, id);
  const value = rawValue != null ? String(rawValue) : "";
  const error = validationErrors.find((e) => e.fieldId === id);

  return (
    <div className="flex flex-col gap-1.5 flex-1 min-w-0">
      {label && (
        <Label htmlFor={id} className="text-sm">
          {translate(label)}
        </Label>
      )}
      <Textarea
        id={id}
        value={value}
        rows={rows}
        maxLength={maxLength}
        className={cn(error && "border-destructive")}
        onChange={(e) => setData({ [id]: e.target.value })}
      />
      {error && (
        <p className="text-xs text-destructive">{error.message}</p>
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
