"use client";

import type { ReactNode } from "react";
import { TableCell, TableRow } from "@/components/ui/table";
import { HighlightedText } from "@/components/shared/highlighted-text";
import type { TimesheetDetail } from "../../types";

/** One column of the recent table: its heading, its share of the fixed width and the value it reads. */
export interface RecentColumn {
  head: string;
  width: string;
  /** The cell's text — highlighted for the search term and the hover text of a truncated cell. */
  cell: (entry: TimesheetDetail) => string;
  /** A cell that is not text (the consumption bar), rendered instead of the highlighted [cell]. */
  render?: (entry: TimesheetDetail) => ReactNode;
}

/** One recent entry as a clickable row — every cell truncates to its column, full text on hover. */
export function RecentRow({
  entry,
  columns,
  query,
  onSelect,
}: {
  entry: TimesheetDetail;
  columns: RecentColumn[];
  /** The search term, highlighted in each cell (see HighlightedText). */
  query: string;
  onSelect: () => void;
}) {
  return (
    <TableRow
      role="button"
      tabIndex={0}
      onClick={onSelect}
      onKeyDown={(e) => {
        if (e.key === "Enter" || e.key === " ") {
          e.preventDefault();
          onSelect();
        }
      }}
      className="cursor-pointer"
    >
      {columns.map((col) => {
        if (col.render) {
          return (
            <TableCell key={col.head} className="px-2 py-1">
              {col.render(entry)}
            </TableCell>
          );
        }
        const value = col.cell(entry);
        return (
          <TableCell
            key={col.head}
            className="truncate px-2 py-1"
            title={value || undefined}
          >
            <HighlightedText text={value} query={query} />
          </TableCell>
        );
      })}
    </TableRow>
  );
}

/** The structure element's name without its trailing task id — "Neukundenakquise (#25207467)" → "Neukundenakquise". */
export function taskName(entry: TimesheetDetail): string {
  return (entry.task?.displayName ?? "").replace(/\s*\(#\d+\)\s*$/, "");
}

/** The consumption of the entry's budget as the backend wrote it out — "12,5PT/20PT (62%)" — or "". */
export function consumptionTitle(entry: TimesheetDetail): string {
  const consumption = entry.task?.consumption as { title?: string } | undefined;
  return consumption?.title ?? "";
}

/**
 * The one lower-cased string a row is matched against — every field it shows (see filterRecent, legacy),
 * the consumption included, so a search for "PT" or a percentage keeps the entries with a budget.
 */
export function searchText(entry: TimesheetDetail): string {
  return [
    entry.task?.displayName,
    consumptionTitle(entry),
    entry.kost2?.displayName,
    entry.kost2?.project?.customer?.name,
    entry.kost2?.project?.name,
    entry.location,
    entry.tag,
    entry.reference,
    entry.description,
  ]
    .filter(Boolean)
    .join(" ")
    .toLowerCase();
}
