"use client";

import { useState, type ReactNode } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Delete02Icon, PlusSignIcon } from "@hugeicons/core-free-icons";
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
 * A nested collection of a hand-built form: the rows, an add button, and a way back to the deleted ones.
 *
 * By default only rows that are not soft-deleted are rendered ([FieldArray.visible]); the deleted ones
 * stay in the form's values and are posted with `deleted = true`, because the backend physically removes
 * whatever a posted collection leaves out. That is also why they can be brought back at all — the row is
 * still there, with its number and its history — and why this component offers to show them: without it,
 * a row deleted by a mis-click is unreachable forever, and its number is spent (see [RepeatableRow]).
 *
 * What a deleted row *looks* like is still the caller's `row`, as for every other row; whether the
 * deleted ones are listed is decided here.
 */
export function RepeatableList<Row extends ArrayRow>({
  array,
  row,
  addLabel,
  onAdd,
  emptyText,
}: RepeatableListProps<Row>) {
  const t = useTranslations();
  const [showDeleted, setShowDeleted] = useState(false);
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
      {/* Below the live rows and after the fold: the deleted ones are the exception a user goes looking
          for, not part of the list they are editing. */}
      {showDeleted &&
        array.deleted.map(([entry, index]) => (
          <div key={index}>{row(entry, index)}</div>
        ))}
      <div className="flex flex-wrap items-center gap-2">
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
        {/* Absent while nothing is deleted: an affordance for an empty set would only raise the question
            what it is for. */}
        {array.deleted.length > 0 && (
          <Button
            type="button"
            variant="ghost"
            size="sm"
            className="self-start text-muted-foreground"
            aria-pressed={showDeleted}
            onClick={() => setShowDeleted((shown) => !shown)}
          >
            <HugeiconsIcon icon={Delete02Icon} size={14} aria-hidden />
            {`${t("deleted")} (${array.deleted.length})`}
          </Button>
        )}
      </div>
    </div>
  );
}
