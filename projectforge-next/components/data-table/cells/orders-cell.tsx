import { MenuLink } from "@/components/shared/menu-link";
import type { CellRenderProps } from "./cell-types";

/** One order of the cell's list, `TaskServicesRest.Task.Order`. */
interface Order {
  number: string;
  title?: string;
  text?: string;
  /** Where the order's page lives — the backend decides, since it may be Wicket's or this app's. */
  url?: string;
}

function isOrder(value: unknown): value is Order {
  return (
    typeof value === "object" &&
    value !== null &&
    typeof (value as Order).number === "string"
  );
}

/**
 * The orders booked against a task: one link per order, comma separated, as
 * Wicket's `OrderPositionsPanel` shows them.
 *
 * A link rather than plain text, and a tooltip per order rather than one for the
 * whole cell — which is why the backend sends the list itself instead of a joined
 * string (`UIAgGridColumnDef` has room for exactly one `tooltipField`).
 */
export function OrdersCell({ value }: CellRenderProps) {
  if (!Array.isArray(value)) return null;
  const orders = value.filter(isOrder);
  if (orders.length === 0) return null;
  return (
    <span className="block truncate">
      {orders.map((order, index) => (
        <span key={order.number}>
          {index > 0 && ", "}
          <MenuLink
            url={order.url}
            className="text-primary hover:underline"
            title={[order.title, order.text].filter(Boolean).join("\n")}
            // The row itself is clickable (it opens the task), so a click on the
            // link must not count as picking the row.
            onClick={(event) => event.stopPropagation()}
          >
            {order.number}
          </MenuLink>
        </span>
      ))}
    </span>
  );
}
