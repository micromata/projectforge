import {
  jsonChanges,
  parseJsonContainer,
  type JsonChange,
} from "@/lib/json-diff";
import type { HistoryEntryAttr } from "@/lib/rs/history";
import { HistoryJsonDiff } from "./history-json-diff";
import { opColor, opSymbol } from "./history-op-style";
import { ChangeArrow, NewValue, OldValue } from "./history-values";

export interface HistoryAttrDiffProps {
  attr: HistoryEntryAttr;
}

/**
 * One changed property: its name and the values before and after.
 *
 * A missing value is left out rather than shown as an empty box — an insert has no old value, a
 * cleared field no new one. Of a list (e.g. the owners of a license) only the removed and added
 * entries are shown, not the whole lists — and of a JSON document (a configuration parameter) only the
 * places that changed, see [HistoryJsonDiff].
 */
export function HistoryAttrDiff({ attr }: HistoryAttrDiffProps) {
  const label = attr.displayPropertyName ?? attr.propertyName ?? "—";
  const symbol = opSymbol(attr.operationType);
  // A list property shows only its removed and added entries (diffed by the backend), each on its
  // own; any other property its whole old and new value.
  const { removedValues, addedValues } = attr;
  const isList = !!removedValues && !!addedValues;
  const json = isList ? null : jsonChangesOf(attr);
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
      {json ? (
        <dd className="w-full">
          <HistoryJsonDiff changes={json} />
        </dd>
      ) : (
        <dd className="flex min-w-0 flex-wrap items-baseline gap-1.5">
          {removed.map((value, i) => (
            <OldValue key={`old-${i}`} value={value} />
          ))}
          {removed.length > 0 && added.length > 0 && <ChangeArrow />}
          {added.map((value, i) => (
            <NewValue key={`new-${i}`} value={value} />
          ))}
        </dd>
      )}
    </div>
  );
}

/**
 * The changes of a property holding a JSON object or array, null for any other value. A side left empty
 * (the first value stored, a value cleared) counts as an empty document of the other's kind. Null as well
 * if nothing changed in the document itself (only its formatting), so the values are shown as they are.
 */
function jsonChangesOf(attr: HistoryEntryAttr): JsonChange[] | null {
  const before = parseJsonContainer(attr.oldValue);
  const after = parseJsonContainer(attr.newValue);
  if (before == null && after == null) return null;
  const emptyLike = (other: unknown) => (Array.isArray(other) ? [] : {});
  if (before == null && attr.oldValue?.trim()) return null;
  if (after == null && attr.newValue?.trim()) return null;
  const changes = jsonChanges(
    before ?? emptyLike(after),
    after ?? emptyLike(before)
  );
  return changes.length > 0 ? changes : null;
}
