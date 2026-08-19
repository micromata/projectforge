"use client";

import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

/**
 * Content that is only shown while the enclosing `Collapsible` is closed.
 *
 * A collapsed row has to say everything its content says — otherwise a folded position or history entry
 * is a row nobody reads back. Unfolded, the very same values are directly below it, and showing them
 * twice reads as two different sets of numbers.
 *
 * Driven by the `data-state` Radix puts on the `Collapsible` root (see `components/ui/collapsible.tsx`)
 * rather than by a prop: the open state belongs to the collapsible, and passing it down would make every
 * header either a function or a second place holding it. This is also what lets a header stay a piece of
 * markup ([RepeatableRow] takes it as a `ReactNode`).
 *
 * A soft-deleted row has no `Collapsible` around it at all ([RepeatableRow] renders it as a header only),
 * so there the summary stays visible — which is right for a row that has no fields to read instead.
 */
export function CollapsedOnly({
  children,
  className,
}: {
  children: ReactNode;
  className?: string;
}) {
  return (
    <span className={cn("[[data-state=open]_&]:hidden", className)}>
      {children}
    </span>
  );
}
