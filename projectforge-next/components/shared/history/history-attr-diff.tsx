import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowRight01Icon } from "@hugeicons/core-free-icons";
import type { HistoryEntryAttr } from "@/lib/rs/history";

export interface HistoryAttrDiffProps {
  attr: HistoryEntryAttr;
}

/**
 * One changed property: its name and the values before and after.
 *
 * A missing value is left out rather than shown as an empty box — an insert has no old value, a
 * cleared field no new one.
 */
export function HistoryAttrDiff({ attr }: HistoryAttrDiffProps) {
  const label = attr.displayPropertyName ?? attr.propertyName ?? "—";
  const hasOld = !!attr.oldValue?.trim();
  const hasNew = !!attr.newValue?.trim();
  return (
    <div className="flex flex-wrap items-baseline gap-x-2 gap-y-1 text-xs leading-relaxed">
      <dt className="font-medium text-foreground/70">{label}</dt>
      <dd className="flex min-w-0 flex-wrap items-baseline gap-1.5">
        {hasOld && (
          <span
            className="rounded px-1.5 py-0.5 line-through decoration-1"
            style={{ background: "var(--history-old-bg)" }}
          >
            {attr.oldValue}
          </span>
        )}
        {hasOld && hasNew && (
          <HugeiconsIcon
            icon={ArrowRight01Icon}
            size={12}
            aria-hidden
            className="shrink-0 self-center text-muted-foreground"
          />
        )}
        {hasNew && (
          <span
            className="rounded px-1.5 py-0.5"
            style={{ background: "var(--history-new-bg)" }}
          >
            {attr.newValue}
          </span>
        )}
      </dd>
    </div>
  );
}
