"use client";

import { useState } from "react";
import { Input } from "@/components/ui/input";

/**
 * The inline name field of a group, for a new group and for renaming one: Enter (or leaving the field)
 * commits a non-blank name, Escape cancels. A blank name commits nothing.
 */
export function GroupNameInput({
  initial = "",
  label,
  onCommit,
  onCancel,
  commitOnBlur,
}: {
  initial?: string;
  /** Accessible name and placeholder. */
  label: string;
  onCommit: (name: string) => void;
  onCancel: () => void;
  commitOnBlur?: boolean;
}) {
  const [name, setName] = useState(initial);
  const commit = () => (name.trim() ? onCommit(name.trim()) : onCancel());
  return (
    <Input
      autoFocus
      value={name}
      aria-label={label}
      placeholder={label}
      className="h-7 min-w-0 flex-1 text-sm"
      onChange={(e) => setName(e.target.value)}
      onPointerDown={(e) => e.stopPropagation()}
      onKeyDown={(e) => {
        // The keyboard sensor of a surrounding sortable must not see the typing.
        e.stopPropagation();
        if (e.key === "Enter") commit();
        if (e.key === "Escape") onCancel();
      }}
      onBlur={commitOnBlur ? commit : undefined}
    />
  );
}
