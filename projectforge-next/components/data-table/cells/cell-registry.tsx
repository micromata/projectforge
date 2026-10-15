import type { ReactNode } from "react";
import { BooleanCell } from "./boolean-cell";
import { ConsumptionCell } from "./consumption-cell";
import { IconCell } from "./icon-cell";
import { OrdersCell } from "./orders-cell";
import { RatingCell } from "./rating-cell";
import { TaskStatusCell } from "./task-status-cell";
import { TextCell } from "./text-cell";
import { TreeCell } from "./tree-cell";
import type { CellKind, CellRenderProps } from "./cell-types";

/**
 * Which component renders which kind of cell. One entry per CellKind, so adding
 * a kind is a compile error until it has a renderer.
 */
const CELL_RENDERERS: Record<CellKind, (props: CellRenderProps) => ReactNode> =
  {
    text: TextCell,
    boolean: BooleanCell,
    rating: RatingCell,
    consumption: ConsumptionCell,
    tree: TreeCell,
    icon: IconCell,
    orders: OrdersCell,
    taskStatus: TaskStatusCell,
  };

/**
 * Renders a cell according to its spec. An unknown kind falls back to text
 * rather than to nothing, so a backend formatter we don't know yet still shows
 * its value.
 */
export function renderCell(props: CellRenderProps): ReactNode {
  const Renderer = CELL_RENDERERS[props.spec.kind] ?? TextCell;
  return <Renderer {...props} />;
}
