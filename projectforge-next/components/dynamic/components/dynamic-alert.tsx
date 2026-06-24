"use client";

import type { DynamicComponentProps } from "../dynamic-renderer";
import { useDynamicLayout } from "../dynamic-context";
import { cn } from "@/lib/utils";

export function DynamicAlert({ node }: DynamicComponentProps) {
  const { translate } = useDynamicLayout();

  const message = (node.message as string) ?? "";
  const color = (node.color as string) ?? "info";

  const colorClasses = cn(
    "rounded-md px-4 py-3 text-sm",
    color === "danger" && "bg-destructive/10 text-destructive",
    color === "warning" && "bg-yellow-50 text-yellow-800 dark:bg-yellow-950 dark:text-yellow-200",
    color === "success" && "bg-green-50 text-green-800 dark:bg-green-950 dark:text-green-200",
    (color === "info" || color === "light") &&
      "bg-muted text-muted-foreground"
  );

  return <div className={colorClasses}>{translate(message)}</div>;
}
