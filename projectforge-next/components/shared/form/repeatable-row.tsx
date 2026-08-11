"use client";

import { useState, type ReactNode } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowDown01Icon, Delete02Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import { cn } from "@/lib/utils";

export interface RepeatableRowProps {
  /** What the collapsed row says — the position's number, title and sum. */
  header: ReactNode;
  children: ReactNode;
  /** Open by default: a row just added is there to be filled in. */
  defaultOpen?: boolean;
  /** Absent when the row must not be removed (an order position already invoiced elsewhere). */
  onRemove?: () => void;
  /** Accessible name of the remove button, naming the row it belongs to. */
  removeLabel: string;
}

/**
 * One row of a [RepeatableList]: a header that stays readable while collapsed, and the fields below it.
 *
 * Collapsing matters here rather than being a nicety — an order with a dozen positions is a page of
 * fields otherwise, which is exactly what makes the legacy form hard to read.
 */
export function RepeatableRow({
  header,
  children,
  defaultOpen,
  onRemove,
  removeLabel,
}: RepeatableRowProps) {
  const t = useTranslations();
  const [open, setOpen] = useState(defaultOpen ?? false);
  return (
    <Collapsible
      open={open}
      onOpenChange={setOpen}
      className="rounded-md border border-border bg-background"
    >
      <div className="flex items-center gap-2 px-3 py-2">
        <CollapsibleTrigger className="flex min-w-0 flex-1 cursor-pointer items-center gap-2 text-left">
          <HugeiconsIcon
            icon={ArrowDown01Icon}
            size={14}
            aria-hidden
            className={cn(
              "shrink-0 text-muted-foreground transition-transform",
              !open && "-rotate-90"
            )}
          />
          <span className="min-w-0 flex-1">{header}</span>
        </CollapsibleTrigger>
        {onRemove && (
          <Button
            type="button"
            variant="ghost"
            size="icon"
            className="size-7 shrink-0 text-muted-foreground hover:text-destructive"
            aria-label={`${t("delete")}: ${removeLabel}`}
            onClick={onRemove}
          >
            <HugeiconsIcon icon={Delete02Icon} size={14} />
          </Button>
        )}
      </div>
      <CollapsibleContent>
        <div className="grid grid-cols-1 gap-x-6 gap-y-4 border-t border-border/60 px-3 py-4 md:grid-cols-3">
          {children}
        </div>
      </CollapsibleContent>
    </Collapsible>
  );
}
