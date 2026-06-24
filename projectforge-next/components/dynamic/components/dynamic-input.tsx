"use client";

import type { DynamicComponentProps } from "../dynamic-renderer";
import { useDynamicLayout } from "../dynamic-context";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";

export function DynamicInput({ node }: DynamicComponentProps) {
  const { data, setData, translate, validationErrors } = useDynamicLayout();

  const id = node.id as string;
  const dataType = (node.dataType as string) ?? "STRING";
  const label = node.label as string | undefined;
  const required = node.required as boolean | undefined;
  const maxLength = node.maxLength as number | undefined;
  const focus = node.focus as boolean | undefined;

  const rawValue = getNestedValue(data, id);
  const value = rawValue != null ? String(rawValue) : "";

  const error = validationErrors.find((e) => e.fieldId === id);

  const inputType = resolveInputType(dataType);

  return (
    <div className="flex flex-col gap-1.5 flex-1 min-w-0">
      {label && (
        <Label htmlFor={id} className="text-sm">
          {translate(label)}
          {required && <span className="text-destructive ml-0.5">*</span>}
        </Label>
      )}
      <Input
        id={id}
        type={inputType}
        value={value}
        autoFocus={focus}
        maxLength={maxLength}
        required={required}
        className={cn(error && "border-destructive")}
        onChange={(e) => setData({ [id]: e.target.value })}
      />
      {error && (
        <p className="text-xs text-destructive">{error.message}</p>
      )}
    </div>
  );
}

function resolveInputType(dataType: string): string {
  switch (dataType) {
    case "INT":
    case "LONG":
    case "DECIMAL":
    case "NUMBER":
      return "number";
    case "PASSWORD":
      return "password";
    case "DATE":
      return "date";
    case "TIME":
      return "time";
    case "TIMESTAMP":
      return "datetime-local";
    default:
      return "text";
  }
}

function getNestedValue(obj: Record<string, unknown>, path: string): unknown {
  return path.split(".").reduce<unknown>((acc, key) => {
    if (acc && typeof acc === "object") return (acc as Record<string, unknown>)[key];
    return undefined;
  }, obj);
}
