"use client";

import type { DynamicComponentProps } from "../../dynamic-renderer";
import { useDynamicLayout } from "../../dynamic-context";
import { DynamicField } from "../dynamic-field";
import { Input } from "@/components/ui/input";
import { getByPath } from "@/lib/dynamic/path";
import { cn } from "@/lib/utils";

/**
 * A DATE input. The wire format of a `LocalDate` is `yyyy-MM-dd` (see LocalDateConverter in
 * projectforge-business), which is exactly what `<input type="date">` reads and writes - so the
 * value passes through untouched and no timezone can shift it by a day.
 */
export function DynamicDateInput({ node }: DynamicComponentProps) {
  const { data, setData } = useDynamicLayout();

  const id = node.id as string;
  const raw = getByPath(data, id);
  // Older endpoints send a full timestamp for a date field; only the date part is editable.
  const value = typeof raw === "string" ? raw.slice(0, 10) : "";

  return (
    <DynamicField node={node}>
      {(domId, hasError) => (
        <Input
          id={domId}
          type="date"
          value={value}
          autoFocus={node.focus as boolean | undefined}
          required={node.required as boolean | undefined}
          className={cn(hasError && "border-destructive")}
          // An empty input must clear the field, not send "".
          onChange={(e) => setData({ [id]: e.target.value || null })}
        />
      )}
    </DynamicField>
  );
}
