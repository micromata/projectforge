"use client";

import { getByPath, type DataObject } from "@/lib/dynamic/path";
import { useDynamicLayout } from "../../dynamic-context";
import type { DynamicComponentProps } from "../../dynamic-renderer";

interface SimpleColumn {
  field?: string;
  id?: string;
  headerName?: string;
  title?: string;
  hide?: boolean;
}

/**
 * A plain table for TABLE nodes that carry `columns` instead of `columnDefs`
 * (UITable, the pre-AG-Grid element — still used by a handful of dynamic pages).
 * Those have no formatter, filter or width information, so there is nothing for
 * the adapter to map; DynamicGrid falls back to this.
 */
export function DynamicGridFallback({ node }: DynamicComponentProps) {
  const { data, translate } = useDynamicLayout();

  const columns = (
    (node.columnDefs ?? node.columns ?? []) as SimpleColumn[]
  ).filter((col) => !col.hide);
  const rows = (data[(node.id as string) ?? "resultSet"] as DataObject[]) ?? [];

  if (columns.length === 0 && rows.length === 0) return null;

  return (
    <div className="w-full overflow-auto rounded-md border">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b bg-muted/50">
            {columns.map((col, index) => (
              <th
                key={col.field ?? col.id ?? index}
                className="px-3 py-2 text-left font-medium text-muted-foreground"
              >
                {translate(
                  col.headerName ?? col.title ?? col.field ?? col.id ?? ""
                )}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, rowIndex) => (
            <tr
              key={rowIndex}
              className="border-b last:border-0 hover:bg-muted/30"
            >
              {columns.map((col, index) => (
                <td key={col.field ?? col.id ?? index} className="px-3 py-2">
                  {cellText(getByPath(row, col.field ?? col.id ?? ""))}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function cellText(value: unknown): string {
  if (value == null) return "";
  if (typeof value === "boolean") return value ? "✓" : "—";
  if (Array.isArray(value)) return value.map(cellText).join(", ");
  if (typeof value === "object") return JSON.stringify(value);
  return String(value);
}
