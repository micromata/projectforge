"use client";

import { useState, type KeyboardEvent } from "react";
import { useTranslations } from "next-intl";
import { cn } from "@/lib/utils";

export interface TagChipProps {
  tag: string;
  /** Classes of the chip's look, chosen by [TagInput]'s variant. */
  chipClassName: string;
  /**
   * Called with the edited text, trimmed. An empty one removes the tag; whether the text may stand
   * (no duplicate) is the caller's to decide.
   */
  onEdit: (next: string) => void;
  onRemove: () => void;
}

/**
 * One value of a [TagInput]: a chip whose text is clicked to correct it in place — a typo in a long
 * customer name is fixed, not deleted and typed anew.
 *
 * Enter or leaving the field takes the text, Escape keeps the former one.
 */
export function TagChip({
  tag,
  chipClassName,
  onEdit,
  onRemove,
}: TagChipProps) {
  const t = useTranslations("select");
  const [draft, setDraft] = useState<string | null>(null);

  const finish = () => {
    if (draft === null) return;
    setDraft(null);
    if (draft.trim() !== tag) onEdit(draft.trim());
  };

  const handleKey = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      // Also keeps the surrounding form from being submitted.
      e.preventDefault();
      finish();
    } else if (e.key === "Escape") {
      // Only the edit is cancelled, not a dialog around it.
      e.preventDefault();
      e.stopPropagation();
      setDraft(null);
    }
  };

  if (draft !== null) {
    return (
      <input
        autoFocus
        aria-label={t("edit", { label: tag })}
        value={draft}
        onChange={(e) => setDraft(e.target.value)}
        onKeyDown={handleKey}
        onBlur={finish}
        // As wide as its text, so the chips around it don't jump while it is edited.
        size={Math.max(draft.length, 4)}
        className={cn(
          "h-6 max-w-full rounded-full border px-2 text-xs font-semibold outline-none focus:ring-2 focus:ring-ring/30",
          chipClassName
        )}
      />
    );
  }

  return (
    <span
      className={cn(
        "inline-flex h-6 max-w-full items-center gap-1 rounded-full border px-2 text-xs font-semibold",
        chipClassName
      )}
    >
      <button
        type="button"
        onClick={() => setDraft(tag)}
        aria-label={t("edit", { label: tag })}
        className="cursor-text truncate"
      >
        {tag}
      </button>
      <button
        type="button"
        onClick={onRemove}
        aria-label={t("remove", { label: tag })}
        className="opacity-60 hover:opacity-100"
      >
        ×
      </button>
    </span>
  );
}
