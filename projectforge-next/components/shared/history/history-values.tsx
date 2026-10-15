import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowRight01Icon } from "@hugeicons/core-free-icons";

/** The old and new values of a history entry, as removed and added chips (see HistoryAttrDiff). */

export function OldValue({ value }: { value: string }) {
  return (
    <span
      className="rounded px-1.5 py-0.5 line-through decoration-1"
      style={{ background: "var(--history-old-bg)" }}
    >
      {value}
    </span>
  );
}

export function NewValue({ value }: { value: string }) {
  return (
    <span
      className="rounded px-1.5 py-0.5"
      style={{ background: "var(--history-new-bg)" }}
    >
      {value}
    </span>
  );
}

export function ChangeArrow() {
  return (
    <HugeiconsIcon
      icon={ArrowRight01Icon}
      size={12}
      aria-hidden
      className="shrink-0 self-center text-muted-foreground"
    />
  );
}
