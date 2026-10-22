import type { ContributionMarginBreakdownRow } from "@/lib/rs/order";

/** Up to this many projects are expanded at first; more are shown collapsed. */
const EXPAND_PROJECTS_UP_TO = 10;

/** The rows expanded at first: the projects, if there are only a few. */
export function initiallyExpanded(
  rows: ContributionMarginBreakdownRow[]
): Set<string> {
  const projects = rows.filter((row) => row.parentId == null);
  return new Set(
    projects.length <= EXPAND_PROJECTS_UP_TO
      ? projects.map((row) => row.id)
      : []
  );
}

/** All rows with children, for "expand all". */
export function expandableIds(
  rows: ContributionMarginBreakdownRow[]
): Set<string> {
  return new Set(rows.filter((row) => row.hasChildren).map((row) => row.id));
}

/**
 * The rows (in tree order, as the backend sends them) whose ancestors are all expanded: a row's parent comes
 * before it, so one pass suffices.
 */
export function visibleRows(
  rows: ContributionMarginBreakdownRow[],
  expanded: Set<string>
): ContributionMarginBreakdownRow[] {
  const visible = new Set<string>();
  return rows.filter((row) => {
    const shown =
      row.parentId == null ||
      (visible.has(row.parentId) && expanded.has(row.parentId));
    if (shown) visible.add(row.id);
    return shown;
  });
}

/** The indentation of a row by its depth in the tree (spelled out for Tailwind), capped at the last one. */
const DEPTH_PADDING = [
  "pl-0",
  "pl-5",
  "pl-10",
  "pl-15",
  "pl-20",
  "pl-25",
  "pl-30",
  "pl-35",
  "pl-40",
];

export function depthPadding(depth: number): string {
  return DEPTH_PADDING[Math.min(depth, DEPTH_PADDING.length - 1)];
}
