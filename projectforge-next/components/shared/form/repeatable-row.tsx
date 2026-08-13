"use client";

import { useState, type ReactNode } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  ArrowDown01Icon,
  ArrowTurnBackwardIcon,
  Delete02Icon,
} from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { cn } from "@/lib/utils";

export interface RepeatableRowProps {
  /** What the collapsed row says — the position's number, title and sum. */
  header: ReactNode;
  children: ReactNode;
  /** Open by default: a row just added is there to be filled in. */
  defaultOpen?: boolean;
  /**
   * Absent when the row must not be removed (an order position already invoiced elsewhere). Runs only
   * after the user confirmed — the row asks for itself.
   */
  onRemove?: () => void;
  /** Accessible name of the remove and the restore button, naming the row it belongs to. */
  removeLabel: string;
  /** Draws attention to rows that require action — e.g. an order position with outstanding invoicing. */
  highlighted?: boolean;
  /**
   * The row is soft-deleted and only shown because the user asked to see the deleted ones
   * ([RepeatableList]): struck through, greyed, and its fields are not rendered at all.
   *
   * Not merely folded away — the fields bind to the form, and a row that is on its way out must not
   * offer values to edit or a tab stop to reach them by.
   */
  deleted?: boolean;
  /**
   * Takes the row back. Only meaningful together with `deleted`, and absent where the user may not
   * write — restoring is the harmless direction, so it needs no confirmation.
   */
  onRestore?: () => void;
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
  highlighted,
  deleted,
  onRestore,
}: RepeatableRowProps) {
  const t = useTranslations();
  const [open, setOpen] = useState(defaultOpen ?? false);
  const [confirming, setConfirming] = useState(false);

  if (deleted) {
    return (
      <div className="flex items-center gap-2 rounded-md border border-dashed border-border bg-muted/40 px-3 py-2">
        <span className="min-w-0 flex-1 text-muted-foreground line-through">
          {header}
        </span>
        {onRestore && (
          <Button
            type="button"
            variant="ghost"
            size="icon"
            className="size-7 shrink-0 text-muted-foreground hover:text-foreground"
            aria-label={`${t("undelete")}: ${removeLabel}`}
            onClick={onRestore}
          >
            <HugeiconsIcon icon={ArrowTurnBackwardIcon} size={14} />
          </Button>
        )}
      </div>
    );
  }

  return (
    <Collapsible
      open={open}
      onOpenChange={setOpen}
      className="rounded-md border border-border bg-background"
    >
      <div
        className={cn(
          "flex items-center gap-2 rounded-t-md px-3 py-2",
          highlighted && "bg-destructive/15"
        )}
      >
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
          <>
            <Button
              type="button"
              variant="ghost"
              size="icon"
              className="size-7 shrink-0 text-muted-foreground hover:text-destructive"
              aria-label={`${t("delete")}: ${removeLabel}`}
              onClick={() => setConfirming(true)}
            >
              <HugeiconsIcon icon={Delete02Icon} size={14} />
            </Button>
            {/* A row is removed without an undo, and the button sits right beside the one that folds the
                row open — a mis-click would silently drop a position or an instalment. The question says
                what actually happens: the row is gone from the form and deleted when it is saved. */}
            <ConfirmDialog
              open={confirming}
              onOpenChange={setConfirming}
              title={`${t("delete")}: ${removeLabel}`}
              description={t("question.deleteRowQuestion")}
              confirmLabel={t("delete")}
              destructive
              onConfirm={onRemove}
            />
          </>
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
