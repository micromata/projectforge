import type { JsonChange } from "@/lib/json-diff";
import { ChangeArrow, NewValue, OldValue } from "./history-values";

export interface HistoryJsonDiffProps {
  changes: JsonChange[];
}

/**
 * The changes of a JSON-valued property (a configuration parameter such as the customer groups), one line
 * per changed place instead of the whole old and new document. The path is shown as it is stored — keys
 * of the JSON, array elements by their name (see lib/json-diff.ts).
 */
export function HistoryJsonDiff({ changes }: HistoryJsonDiffProps) {
  return (
    <ul className="flex w-full flex-col gap-1 border-l border-border pl-3">
      {changes.map((change) => (
        <li
          key={change.path.join("\u0000")}
          className="flex flex-wrap items-baseline gap-x-2 gap-y-1"
        >
          <span className="font-mono text-foreground/70">
            {change.path.join(" › ") || "/"}
          </span>
          <span className="flex min-w-0 flex-wrap items-baseline gap-1.5">
            {change.removed.map((value, i) => (
              <OldValue key={`old-${i}`} value={value} />
            ))}
            {change.removed.length > 0 && change.added.length > 0 && (
              <ChangeArrow />
            )}
            {change.added.map((value, i) => (
              <NewValue key={`new-${i}`} value={value} />
            ))}
          </span>
        </li>
      ))}
    </ul>
  );
}
