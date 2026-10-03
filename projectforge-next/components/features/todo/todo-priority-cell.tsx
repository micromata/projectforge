"use client";

import { useTranslations } from "next-intl";
import { TO_DO_METADATA } from "@/lib/metadata/to-do.generated";
import { cn } from "@/lib/utils";
import { fromMetadata } from "@/lib/validation/from-metadata";
import type { Priority } from "./types";

const m = fromMetadata(TO_DO_METADATA);

/** Wicket's `priority_high` / `priority_highest` styles; the lower priorities stay plain. */
const PRIORITY_CLASS: Partial<Record<Priority, string>> = {
  HIGH: "font-bold text-destructive",
  HIGHEST: "rounded-sm bg-destructive px-1 font-bold text-white",
};

/** The priority of a to-do in the list, the high ones stand out as on the Wicket list. */
export function ToDoPriorityCell({ priority }: { priority?: Priority | null }) {
  const t = useTranslations();
  if (!priority) return null;
  const label =
    m.enumOptions("priority", t).find((o) => o.value === priority)?.label ??
    priority;
  return <span className={cn(PRIORITY_CLASS[priority])}>{label}</span>;
}
