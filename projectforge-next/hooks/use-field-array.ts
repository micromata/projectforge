"use client";

import { useCallback } from "react";
import { useStore } from "@tanstack/react-form";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import {
  type ArrayRow,
  readArrayAtPath,
  removeRow,
  restoreRow,
  updateRow,
} from "@/lib/field-array";

export type { ArrayRow };

export interface FieldArray<Row extends ArrayRow> {
  /** Every row, deleted ones included — the list that is posted. */
  rows: Row[];
  /** `[row, index]` of the rows a user sees, i.e. everything not soft-deleted. */
  visible: [Row, number][];
  /**
   * `[row, index]` of the soft-deleted rows — what a restore affordance offers (see [restore]).
   *
   * Kept apart from [visible] rather than left to the caller: a row is identified by its index in the
   * values, so pairing the two has to happen where the array is (see [fieldName]).
   */
  deleted: [Row, number][];
  /** Name prefix of the fields of one row, e.g. `positionen[2].` — what the field components bind to. */
  fieldName: (index: number, field: string) => string;
  /** Appends a row and returns its index. */
  add: (row: Row) => number;
  /**
   * Marks a row deleted, or drops it outright when it was never saved: an unsaved row has nothing in
   * the database to soft-delete, and keeping it would post an empty position.
   */
  remove: (index: number) => void;
  /** Takes a deleted row back — the counterpart of [remove] for a row that still exists in the list. */
  restore: (index: number) => void;
  update: (index: number, changes: Partial<Row>) => void;
}

/**
 * The mechanics of a nested collection in a hand-built form: which rows there are, adding, soft
 * deleting, and the field name of a row's fields.
 *
 * Deliberately not a concept of the page declaration (`lib/page-def/types.ts`): what a row *looks*
 * like is the entity's own business — an order position has a title, sums, a period of performance and
 * a task — and describing that declaratively would be a second form framework. So a page renders its
 * rows as plain JSX and takes only the mechanics from here (see [RepeatableList]).
 *
 * @param name Path of the form value holding the array. A plain name for a collection of the entity
 *   (`positionen`), or a bracketed path for one nested inside a row of another — the invoice form's
 *   `positionen[0].kostZuweisungen`, its second nesting level. Both are read through
 *   [readArrayAtPath], and `form.setFieldValue` accepts either as it is (it is TanStack's own path
 *   syntax).
 */
export function useFieldArray<Row extends ArrayRow>(
  name: string
): FieldArray<Row> {
  const form = useEntityEditForm();
  const rows = useStore(form.store, (state) =>
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    readArrayAtPath<Row>((state as any).values, name)
  ) as Row[];

  const setRows = useCallback(
    (next: (previous: Row[]) => Row[]) => {
      form.setFieldValue(name, (previous: Row[] | undefined) =>
        next(previous ?? [])
      );
    },
    [form, name]
  );

  const add = useCallback(
    (row: Row) => {
      let index = 0;
      setRows((previous) => {
        index = previous.length;
        return [...previous, row];
      });
      return index;
    },
    [setRows]
  );

  const remove = useCallback(
    (index: number) => setRows((previous) => removeRow(previous, index)),
    [setRows]
  );

  const restore = useCallback(
    (index: number) => setRows((previous) => restoreRow(previous, index)),
    [setRows]
  );

  const update = useCallback(
    (index: number, changes: Partial<Row>) =>
      setRows((previous) => updateRow(previous, index, changes)),
    [setRows]
  );

  const indexed = rows.map((row, index) => [row, index] as [Row, number]);

  return {
    rows,
    visible: indexed.filter(([row]) => !row.deleted),
    deleted: indexed.filter(([row]) => row.deleted),
    fieldName: (index, field) => `${name}[${index}].${field}`,
    add,
    remove,
    restore,
    update,
  };
}
