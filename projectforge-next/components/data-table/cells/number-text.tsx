import type { ReactNode } from "react";
import { cellClassFor } from "@/lib/cell-styles";

/** A number in a table cell (amount, decimal, whole number): see `NUMERIC_CELL_CLASS`. */
export function NumericText({
  children,
  className,
}: {
  children: ReactNode;
  className?: string;
}) {
  return <span className={cellClassFor("numeric", className)}>{children}</span>;
}

/** A cost unit number in a table cell (`5.100.01.02`): see `COST_CELL_CLASS`. */
export function CostText({
  children,
  className,
}: {
  children: ReactNode;
  className?: string;
}) {
  return <span className={cellClassFor("cost", className)}>{children}</span>;
}
