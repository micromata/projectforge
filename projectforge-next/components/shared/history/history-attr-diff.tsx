import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowRight01Icon } from "@hugeicons/core-free-icons";
import type { HistoryEntryAttr } from "@/lib/rs/history";
import { opColor, opSymbol } from "./history-op-style";

export interface HistoryAttrDiffProps {
  attr: HistoryEntryAttr;
}

/**
 * One changed property: its name and the values before and after.
 *
 * A missing value is left out rather than shown as an empty box — an insert has no old value, a
 * cleared field no new one. Of a list (e.g. the owners of a license) only the removed and added
 * entries are shown, not the whole lists.
 */
export function HistoryAttrDiff({ attr }: HistoryAttrDiffProps) {
  const label = attr.displayPropertyName ?? attr.propertyName ?? "—";
  const symbol = opSymbol(attr.operationType);
  // A list property shows only its removed and added entries (diffed by the backend), each on its
  // own; any other property its whole old and new value.
  const { removedValues, addedValues } = attr;
  const isList = !!removedValues && !!addedValues;
  const removed = isList
    ? removedValues
    : attr.oldValue?.trim()
      ? [attr.oldValue]
      : [];
  const added = isList
    ? addedValues
    : attr.newValue?.trim()
      ? [attr.newValue]
      : [];
  return (
    <div className="flex flex-wrap items-baseline gap-x-2 gap-y-1 text-xs leading-relaxed">
      <dt className="font-medium text-foreground/70">
        {symbol && (
          <span
            aria-hidden
            className="mr-1 font-bold"
            style={{ color: opColor(attr.operationType) }}
          >
            {symbol}
          </span>
        )}
        {label}
      </dt>
      <dd className="flex min-w-0 flex-wrap items-baseline gap-1.5">
        {removed.map((value, i) => (
          <OldValue key={`old-${i}`} value={value} />
        ))}
        {removed.length > 0 && added.length > 0 && <ChangeArrow />}
        {added.map((value, i) => (
          <NewValue key={`new-${i}`} value={value} />
        ))}
      </dd>
    </div>
  );
}

function OldValue({ value }: { value: string }) {
  return (
    <span
      className="rounded px-1.5 py-0.5 line-through decoration-1"
      style={{ background: "var(--history-old-bg)" }}
    >
      {value}
    </span>
  );
}

function NewValue({ value }: { value: string }) {
  return (
    <span
      className="rounded px-1.5 py-0.5"
      style={{ background: "var(--history-new-bg)" }}
    >
      {value}
    </span>
  );
}

function ChangeArrow() {
  return (
    <HugeiconsIcon
      icon={ArrowRight01Icon}
      size={12}
      aria-hidden
      className="shrink-0 self-center text-muted-foreground"
    />
  );
}
