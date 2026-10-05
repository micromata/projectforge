"use client";

import type { ComponentProps } from "react";
import { Select as SelectPrimitive } from "radix-ui";
import { HugeiconsIcon } from "@hugeicons/react";
import { Tick02Icon } from "@hugeicons/core-free-icons";
import { cn } from "@/lib/utils";

/**
 * A Select option that carries a one-line explanation under its label — the generic form of what the
 * liquidity "automatic" paid state needs (see PaidSelectField).
 *
 * The hint sits *inside* the Radix item, so clicking it selects the option exactly as clicking the label
 * does, but *outside* the `ItemText` Radix mirrors into the trigger, so the closed combobox shows only the
 * label and never the (longer) hint. A plain option with no explanation stays on shadcn's `SelectItem`;
 * this is only for the few that need the extra line.
 *
 * Mirrors `components/ui/select.tsx`'s `SelectItem` (which cannot be edited) except for the stacked layout
 * and the hint slot.
 */
export function SelectItemWithHint({
  hint,
  hintClassName,
  children,
  className,
  ...props
}: ComponentProps<typeof SelectPrimitive.Item> & {
  hint?: string;
  /** E.g. a wider hint for an explanation of several sentences. */
  hintClassName?: string;
}) {
  return (
    <SelectPrimitive.Item
      data-slot="select-item"
      className={cn(
        "relative flex w-full cursor-default flex-col rounded-md py-1 pr-8 pl-2 text-xs/relaxed outline-hidden select-none focus:bg-accent focus:text-accent-foreground data-disabled:pointer-events-none data-disabled:opacity-50 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-3.5",
        className
      )}
      {...props}
    >
      <span className="pointer-events-none absolute top-1.5 right-2 flex items-center justify-center">
        <SelectPrimitive.ItemIndicator>
          <HugeiconsIcon icon={Tick02Icon} strokeWidth={2} />
        </SelectPrimitive.ItemIndicator>
      </span>
      <SelectPrimitive.ItemText>{children}</SelectPrimitive.ItemText>
      {hint ? (
        // Not pointer-events-none: a click here must still reach the item and select it.
        <span
          className={cn(
            "mt-0.5 max-w-64 text-[11px] leading-snug text-muted-foreground",
            hintClassName
          )}
        >
          {hint}
        </span>
      ) : null}
    </SelectPrimitive.Item>
  );
}
