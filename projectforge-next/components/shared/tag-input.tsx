"use client";

import { useState, type KeyboardEvent } from "react";
import { cn } from "@/lib/utils";

export interface TagInputProps {
  value: string[];
  onChange: (tags: string[]) => void;
  placeholder?: string;
  /** Visual style of the chips. */
  variant?: "primary" | "neutral";
  className?: string;
  inputAriaLabel: string;
}

export function TagInput({
  value,
  onChange,
  placeholder,
  variant = "primary",
  className,
  inputAriaLabel,
}: TagInputProps) {
  const [draft, setDraft] = useState("");

  const commit = (raw: string) => {
    const trimmed = raw.trim();
    if (!trimmed || value.includes(trimmed)) return;
    onChange([...value, trimmed]);
    setDraft("");
  };

  const handleKey = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter" || e.key === ",") {
      e.preventDefault();
      commit(draft);
    } else if (e.key === "Backspace" && !draft && value.length > 0) {
      onChange(value.slice(0, -1));
    }
  };

  const remove = (idx: number) => {
    onChange(value.filter((_, i) => i !== idx));
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
      {value.map((tag, i) => (
        <span
          key={`${tag}-${i}`}
          className={cn(
            "inline-flex h-6 items-center gap-1 rounded-full border px-2 text-xs font-semibold",
            chipClasses
          )}
        >
          {tag}
          <button
            type="button"
            onClick={() => remove(i)}
            aria-label={`${tag} entfernen`}
            className="opacity-60 hover:opacity-100"
          >
            ×
          </button>
        </span>
      ))}
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
