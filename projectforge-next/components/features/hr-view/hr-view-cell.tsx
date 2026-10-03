"use client";

import { MenuLink } from "@/components/shared/menu-link";
import { TableCell } from "@/components/ui/table";
import { useFormatContext } from "@/hooks/use-format";
import { formatNumber } from "@/lib/format";
import type { HrViewCell as Cell } from "./types";

/**
 * One cell of the matrix as the Wicket view wrote it: the planned days, the booked ones in brackets.
 * The booked days lead to their time sheets where `bookedUrl` is given (the sum and the projects; a
 * customer or the rest is no single task).
 */
export function HrViewCell({
  cell,
  bookedUrl,
  className,
}: {
  cell: Cell | undefined;
  bookedUrl?: string;
  className?: string;
}) {
  const ctx = useFormatContext();
  const planned = cell?.planned;
  const actual = cell?.actual;
  const booked = actual != null && `(${formatNumber(actual, ctx, 2)})`;

  return (
    <TableCell className={className ?? "text-right tabular-nums"}>
      {planned != null && formatNumber(planned, ctx, 2)}
      {planned != null && booked && " "}
      {booked &&
        (bookedUrl ? (
          <MenuLink url={bookedUrl} className="text-primary hover:underline">
            {booked}
          </MenuLink>
        ) : (
          <span className="text-muted-foreground">{booked}</span>
        ))}
    </TableCell>
  );
}
