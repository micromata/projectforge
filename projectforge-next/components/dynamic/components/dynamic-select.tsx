"use client";

import type { DynamicComponentProps } from "../dynamic-renderer";
import { useDynamicLayout } from "../dynamic-context";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

interface SelectValue {
  value: string;
  label: string;
}

export function DynamicSelect({ node }: DynamicComponentProps) {
  const { data, setData, translate } = useDynamicLayout();

  const id = node.id as string;
  const label = node.label as string | undefined;
  const values = (node.values as SelectValue[]) ?? [];
  const required = node.required as boolean | undefined;

  const currentValue = String(getNestedValue(data, id) ?? "");

  return (
    <div className="flex flex-col gap-1.5 flex-1 min-w-0">
      {label && (
        <Label htmlFor={id} className="text-sm">
          {translate(label)}
          {required && <span className="text-destructive ml-0.5">*</span>}
        </Label>
      )}
      <Select value={currentValue} onValueChange={(v) => setData({ [id]: v })}>
        <SelectTrigger id={id}>
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          {values.map((opt) => (
            <SelectItem key={opt.value} value={opt.value}>
              {opt.label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  );
}

function getNestedValue(obj: Record<string, unknown>, path: string): unknown {
  return path.split(".").reduce<unknown>((acc, key) => {
    if (acc && typeof acc === "object") return (acc as Record<string, unknown>)[key];
    return undefined;
  }, obj);
}
