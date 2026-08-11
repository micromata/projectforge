"use client";

import type { ReactNode } from "react";
import { HugeiconsIcon } from "@hugeicons/react";
import { PlusSignIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import type { ArrayRow, FieldArray } from "@/hooks/use-field-array";

export interface RepeatableListProps<Row extends ArrayRow> {
  array: FieldArray<Row>;
  /**
   * Renders one row — plain JSX, using `array.fieldName(index, …)` to bind its fields. The row's
   * *content* is the entity's own business; this component only brings adding, ordering and the empty
   * state (see [useFieldArray] for why there is no declarative row).
   */
  row: (entry: Row, index: number) => ReactNode;
  /** Label of the add button, e.g. "Add position". Absent means the list cannot be added to. */
  addLabel?: string;
  onAdd?: () => void;
  /** Shown while nothing is in the list — an order without positions is a normal starting point. */
  emptyText: string;
}

/**
 * A nested collection of a hand-built form: the rows, an add button, nothing else.
 *
 * Only rows that are not soft-deleted are rendered ([FieldArray.visible]); the deleted ones stay in the
 * form's values and are posted with `deleted = true`, because the backend physically removes whatever
 * a posted collection leaves out.
 */
export function RepeatableList<Row extends ArrayRow>({
  array,
  row,
  addLabel,
  onAdd,
  emptyText,
}: RepeatableListProps<Row>) {
  return (
    <div className="flex flex-col gap-2">
      {array.visible.length === 0 ? (
        <p className="py-2 text-xs text-muted-foreground">{emptyText}</p>
      ) : (
        array.visible.map(([entry, index]) => (
          // Keyed by the position in the values, not by the entity's id: a row that has not been saved
          // yet has none, and two of them would collide.
          <div key={index}>{row(entry, index)}</div>
        ))
      )}
      {addLabel && onAdd && (
        <Button
          type="button"
          variant="outline"
          size="sm"
          className="self-start"
          onClick={onAdd}
        >
          <HugeiconsIcon icon={PlusSignIcon} size={14} aria-hidden />
          {addLabel}
        </Button>
      )}
    </div>
  );
}
