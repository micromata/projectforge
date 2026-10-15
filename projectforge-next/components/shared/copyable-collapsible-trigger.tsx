"use client";

import * as React from "react";
import { CollapsibleTrigger as UiCollapsibleTrigger } from "@/components/ui/collapsible";
import {
  COPYABLE_TRIGGER_CLASS,
  useCopyableTrigger,
} from "@/lib/text-selection";
import { cn } from "@/lib/utils";

/**
 * The shadcn `CollapsibleTrigger` (components/ui/collapsible.tsx) with its title selectable and
 * copyable by mouse: the click that ends a drag across it leaves the section as it was (see
 * useCopyableTrigger). Same name and props, so a caller only changes the import.
 */
export function CollapsibleTrigger({
  className,
  onClick,
  onPointerDown,
  ...props
}: React.ComponentProps<typeof UiCollapsibleTrigger>) {
  const copyable = useCopyableTrigger();
  return (
    <UiCollapsibleTrigger
      className={cn(COPYABLE_TRIGGER_CLASS, className)}
      onPointerDown={(event) => {
        onPointerDown?.(event);
        copyable.onPointerDown(event);
      }}
      onClick={(event) => {
        onClick?.(event);
        copyable.onClick(event);
      }}
      {...props}
    />
  );
}
