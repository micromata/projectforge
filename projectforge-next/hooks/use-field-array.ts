"use client";

import { useCallback } from "react";
import { useStore } from "@tanstack/react-form";
import { useEntityEditForm } from "@/components/shared/form/form-context";

/** The least a row of a nested collection has: its identity and whether it is deleted. */
export interface ArrayRow {
  /** null for a row that has not been saved yet — the backend assigns the id. */
  id?: number | null;
  /**
   * Soft delete. A removed row is **kept** in the values with this set, never spliced out: the
   * backend's `CollectionHandler` physically deletes (history and all) whatever is missing from the
   * posted collection, so "deleted" has to be said explicitly. See `AuftragsPosition` in
   * projectforge-rest for the whole story.
   */
  deleted?: boolean;
}

export interface FieldArray<Row extends ArrayRow> {
  /** Every row, deleted ones included — the list that is posted. */
  rows: Row[];
  /** `[row, index]` of the rows a user sees, i.e. everything not soft-deleted. */
  visible: [Row, number][];
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
 * @param name The form value holding the array, e.g. `positionen`.
 */
export function useFieldArray<Row extends ArrayRow>(
  name: string
): FieldArray<Row> {
  const form = useEntityEditForm();
  const rows = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => (state.values?.[name] as Row[] | undefined) ?? []
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
    (index: number) => {
      setRows((previous) => {
        const row = previous[index];
        if (!row) return previous;
        if (row.id == null) {
          return previous.filter((_, at) => at !== index);
        }
        return previous.map((entry, at) =>
          at === index ? { ...entry, deleted: true } : entry
        );
      });
    },
    [setRows]
  );

  const restore = useCallback(
    (index: number) => {
      setRows((previous) =>
        previous.map((entry, at) =>
          at === index ? { ...entry, deleted: false } : entry
        )
      );
    },
    [setRows]
  );

  const update = useCallback(
    (index: number, changes: Partial<Row>) => {
      setRows((previous) =>
        previous.map((entry, at) =>
          at === index ? { ...entry, ...changes } : entry
        )
      );
    },
    [setRows]
  );

  return {
    rows,
    visible: rows
      .map((row, index) => [row, index] as [Row, number])
      .filter(([row]) => !row.deleted),
    fieldName: (index, field) => `${name}[${index}].${field}`,
    add,
    remove,
    restore,
    update,
  };
}
