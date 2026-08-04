import type { RowData } from "@tanstack/react-table";

declare module "@tanstack/react-table" {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars -- required by the module's signature
  interface ColumnMeta<TData extends RowData, TValue> {
    /**
     * Plain-text column name. `header` renders a component (sort button, filter
     * popover), so it can't be reused where only text works — the column panel,
     * aria labels, an export.
     */
    label?: string;
    /** Right-align numeric columns. */
    align?: "left" | "right";
  }
}

export {};
