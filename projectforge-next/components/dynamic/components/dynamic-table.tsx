"use client";

import type { DynamicComponentProps } from "../dynamic-renderer";
import { useDynamicLayout } from "../dynamic-context";

interface ColumnDef {
  id: string;
  title?: string;
  dataType?: string;
}

export function DynamicTable({ node }: DynamicComponentProps) {
  const { data, translate } = useDynamicLayout();

  const columns = (node.columns ?? node.columnDefs ?? []) as ColumnDef[];
  const dataProperty = (node.id as string) ?? "resultSet";
  const rows = (data[dataProperty] as Record<string, unknown>[]) ?? [];

  if (columns.length === 0 && rows.length === 0) {
    return <div className="text-sm text-muted-foreground p-4">No data</div>;
  }

  return (
    <div className="w-full overflow-auto rounded-md border">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b bg-muted/50">
            {columns.map((col) => (
              <th
                key={col.id}
                className="px-3 py-2 text-left font-medium text-muted-foreground"
              >
                {col.title ? translate(col.title) : col.id}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, idx) => (
            <tr key={idx} className="border-b last:border-0 hover:bg-muted/30">
              {columns.map((col) => (
                <td key={col.id} className="px-3 py-2">
                  {formatCellValue(row[col.id])}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function formatCellValue(value: unknown): string {
  if (value == null) return "";
  if (typeof value === "boolean") return value ? "✓" : "—";
  if (typeof value === "object") return JSON.stringify(value);
  return String(value);
}
