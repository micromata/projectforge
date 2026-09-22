"use client";

import type { DynamicComponentProps } from "../../dynamic-renderer";
import { useDynamicLayout } from "../../dynamic-context";
import { DynamicField } from "../dynamic-field";
import { DateInput } from "@/components/shared/date-input";
import { getByPath } from "@/lib/dynamic/path";

/**
 * A DATE input. The wire format of a `LocalDate` is `yyyy-MM-dd` (see LocalDateConverter in
 * projectforge-business), which is exactly what [DateInput] reads and writes - so the value passes
 * through untouched and no timezone can shift it by a day. The layout the user types in comes from
 * their own settings, not the browser's.
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
        <DateInput
          id={domId}
          value={value}
          autoFocus={node.focus as boolean | undefined}
          required={node.required as boolean | undefined}
          invalid={hasError}
          // An empty input must clear the field, not send "".
          onChange={(next) => setData({ [id]: next })}
        />
      )}
    </DynamicField>
  );
}
