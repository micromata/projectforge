"use client";

import type { DynamicComponentProps } from "../dynamic-renderer";
import { useDynamicLayout } from "../dynamic-context";

interface ColumnDef {
  field?: string;
  id?: string;
  headerName?: string;
  title?: string;
  hide?: boolean;
}

export function DynamicTable({ node }: DynamicComponentProps) {
  const { data, translate } = useDynamicLayout();

  const allColumns = (node.columnDefs ?? node.columns ?? []) as ColumnDef[];
  const columns = allColumns.filter((col) => !col.hide);
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
            {columns.map((col, i) => {
              const key = col.field ?? col.id ?? String(i);
              const label = col.headerName ?? col.title ?? col.field ?? col.id ?? "";
              return (
                <th
                  key={key}
                  className="px-3 py-2 text-left font-medium text-muted-foreground"
                >
                  {translate(label)}
                </th>
              );
            })}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, idx) => (
            <tr key={idx} className="border-b last:border-0 hover:bg-muted/30">
              {columns.map((col, i) => {
                const field = col.field ?? col.id ?? "";
                return (
                  <td key={field || i} className="px-3 py-2">
                    {formatCellValue(resolveField(row, field))}
                  </td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function resolveField(row: Record<string, unknown>, field: string): unknown {
  if (!field) return undefined;
  const parts = field.split(".");
  let value: unknown = row;
  for (const part of parts) {
    if (value == null || typeof value !== "object") return undefined;
    value = (value as Record<string, unknown>)[part];
  }
  return value;
}

function formatCellValue(value: unknown): string {
  if (value == null) return "";
  if (typeof value === "boolean") return value ? "✓" : "—";
  if (Array.isArray(value)) return value.map(formatCellValue).join(", ");
  if (typeof value === "object") return JSON.stringify(value);
  return String(value);
}
