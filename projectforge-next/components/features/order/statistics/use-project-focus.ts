"use client";

import { useMemo } from "react";

/**
 * The row to mark and scroll to in a table of the order statistics opened for one project (by a click in
 * a project table): the project's first row matching the first of `preferred` that any row matches (e.g.
 * one with a warning), else its first row; null if the project has none.
 *
 * The tables key their rows by their index, which is what is returned, and open unsorted.
 */
export function useProjectFocus<T>(
  rows: T[],
  projectId: number | null | undefined,
  projectIdOf: (row: T) => number | null,
  preferred: ((row: T) => boolean)[] = []
): number | null {
  return useMemo(() => {
    if (projectId == null) return null;
    const indexes = rows.flatMap((row, index) =>
      projectIdOf(row) === projectId ? [index] : []
    );
    if (indexes.length === 0) return null;
    for (const matches of preferred) {
      const index = indexes.find((it) => matches(rows[it]));
      if (index != null) return index;
    }
    return indexes[0];
    // The callbacks are inline at the call sites and only read the rows.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [rows, projectId]);
}
