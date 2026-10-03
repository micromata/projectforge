"use client";

import { useState, type KeyboardEvent } from "react";
import { useFormatContext } from "@/hooks/use-format";
import { compareText, type FormatContext } from "@/lib/format";
import { cn } from "@/lib/utils";
import { TagChip } from "./tag-chip";

export interface TagInputProps {
  value: string[];
  onChange: (tags: string[]) => void;
  placeholder?: string;
  /** Visual style of the chips. */
  variant?: "primary" | "neutral";
  className?: string;
  inputAriaLabel: string;
  /**
   * Whether a comma confirms the entry as Enter does. Off for values that may contain one themselves —
   * a customer's name ("ACME, Inc.").
   */
  commitOnComma?: boolean;
  /**
   * Shows the chips alphabetically, whatever order they were entered in. The value keeps its order, so
   * sorting marks no form dirty.
   */
  sorted?: boolean;
}

export function TagInput({
  value,
  onChange,
  placeholder,
  variant = "primary",
  className,
  inputAriaLabel,
  commitOnComma = true,
  sorted,
}: TagInputProps) {
  const format = useFormatContext();
  const [draft, setDraft] = useState("");

  const commit = (raw: string) => {
    const trimmed = raw.trim();
    if (!trimmed || value.includes(trimmed)) return;
    onChange([...value, trimmed]);
    setDraft("");
  };

  const handleKey = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter" || (commitOnComma && e.key === ",")) {
      e.preventDefault();
      commit(draft);
    } else if (e.key === "Backspace" && !draft && value.length > 0) {
      onChange(value.slice(0, -1));
    }
  };

  const remove = (idx: number) => {
    onChange(value.filter((_, i) => i !== idx));
  };

  /** Emptied, the tag goes; a text another tag already has is dropped, as [commit] drops it. */
  const edit = (idx: number, next: string) => {
    if (!next) {
      remove(idx);
    } else if (!value.some((tag, i) => i !== idx && tag === next)) {
      onChange(value.map((tag, i) => (i === idx ? next : tag)));
    }
  };

  const chipClasses =
    variant === "primary"
      ? "border-primary/25 bg-primary/10 text-primary"
      : "border-border bg-muted text-foreground";

  return (
    <div
      className={cn(
        "flex min-h-9 flex-wrap items-center gap-1.5 rounded-md border border-input bg-background px-2 py-1 text-sm focus-within:border-ring focus-within:ring-2 focus-within:ring-ring/30",
        className
      )}
    >
      {chipOrder(value, sorted ? format : null).map((i) => {
        const tag = value[i];
        return (
          <TagChip
            key={`${tag}-${i}`}
            tag={tag}
            chipClassName={chipClasses}
            onEdit={(next) => edit(i, next)}
            onRemove={() => remove(i)}
          />
        );
      })}
      <input
        aria-label={inputAriaLabel}
        value={draft}
        onChange={(e) => setDraft(e.target.value)}
        onKeyDown={handleKey}
        onBlur={() => draft && commit(draft)}
        placeholder={value.length === 0 ? placeholder : ""}
        className="min-w-24 flex-1 bg-transparent text-sm outline-none placeholder:text-muted-foreground"
      />
    </div>
  );
}

/** Indices into [value] in display order: as entered, or alphabetically for a sorted input. */
function chipOrder(value: string[], format: FormatContext | null): number[] {
  const indices = value.map((_, i) => i);
  return format
    ? indices.sort((a, b) => compareText(value[a], value[b], format))
    : indices;
}
