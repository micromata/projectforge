"use client";

import { useState } from "react";
import { HugeiconsIcon } from "@hugeicons/react";
import { Cancel01Icon } from "@hugeicons/core-free-icons";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { HintTooltip } from "@/components/shared/hint-tooltip";

/**
 * Logger categories as chips (`org.projectforge.business.fibu`, `org.hibernate`), a new one entered and added
 * by Enter.
 */
export function LoggerChips({
  id,
  label,
  hint,
  placeholder,
  removeLabel,
  value,
  onChange,
}: {
  id: string;
  label: string;
  hint: string;
  placeholder: string;
  /** The accessible name of a chip's remove button. */
  removeLabel: string;
  value: string[];
  onChange: (value: string[]) => void;
}) {
  const [draft, setDraft] = useState("");
  const add = () => {
    const pattern = draft.trim();
    if (pattern && !value.includes(pattern)) onChange([...value, pattern]);
    setDraft("");
  };
  return (
    <div className="flex flex-wrap items-center gap-1.5">
      <HintTooltip text={hint}>
        <Label htmlFor={id}>{label}</Label>
      </HintTooltip>
      {value.map((pattern) => (
        <Badge key={pattern} variant="outline" className="gap-1 font-mono">
          {pattern}
          <button
            type="button"
            aria-label={`${removeLabel}: ${pattern}`}
            className="cursor-pointer text-muted-foreground hover:text-foreground"
            onClick={() => onChange(value.filter((other) => other !== pattern))}
          >
            <HugeiconsIcon icon={Cancel01Icon} className="size-3" />
          </button>
        </Badge>
      ))}
      <Input
        id={id}
        value={draft}
        placeholder={placeholder}
        className="h-7 w-44 text-xs"
        onChange={(event) => setDraft(event.target.value)}
        onBlur={add}
        onKeyDown={(event) => {
          if (event.key === "Enter") {
            event.preventDefault();
            add();
          }
        }}
      />
    </div>
  );
}
