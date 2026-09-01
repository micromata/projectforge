"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import {
  ArrowLeft01Icon,
  ArrowRight01Icon,
  Cancel01Icon,
} from "@hugeicons/core-free-icons";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { cn } from "@/lib/utils";

interface FilterPillShellProps {
  label: string;
  /** The value as text, appended after the label: "Modified: Kai Reinhard, …". */
  text?: string;
  tooltip?: string;
  /** Filled pills read as solid, empty ones as a dashed outline. */
  active: boolean;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** Default filters stay on the row, so they only offer emptying, not removing. */
  removable: boolean;
  /** Restores the value the popover opened with — edits apply live, so there is no "save". */
  onCancel: () => void;
  onDelete: () => void;
  /** Wider than the default for a pill holding more than one field. */
  contentClassName?: string;
  /**
   * Pages the pill's period without opening the popover — set only for a period filter with something to
   * page (see [FilterPill]). Then two arrows flank the label, so the statistics above the list stay in
   * view while the user steps month by month.
   */
  onStep?: (steps: number) => void;
  stepPreviousLabel?: string;
  stepNextLabel?: string;
  /** The input(s) in the popover. */
  children: React.ReactNode;
}

/**
 * The chrome of a filter pill: the trigger, the popover, the remove button and the
 * cancel/delete footer.
 *
 * Shared so that a pill standing for one backend field ([FilterPill]) and the one standing for the
 * three grouped history fields ([HistoryFilterPill]) are the same thing on screen and by keyboard —
 * only their contents and what they apply differ.
 */
export function FilterPillShell({
  label,
  text,
  tooltip,
  active,
  open,
  onOpenChange,
  removable,
  onCancel,
  onDelete,
  contentClassName,
  onStep,
  stepPreviousLabel,
  stepNextLabel,
  children,
}: FilterPillShellProps) {
  const t = useTranslations("filter");
  const tAction = useTranslations();

  // The arrows are for paging with the popover shut; while it is open the in-popover stepper is right
  // there, so showing them too would only double the control (and its accessible name) on screen.
  const stepping = onStep && !open;

  const stepButton = (
    steps: number,
    icon: typeof ArrowLeft01Icon,
    label?: string
  ) => (
    <button
      type="button"
      onClick={() => onStep?.(steps)}
      aria-label={label}
      title={label}
      className="flex size-4 shrink-0 cursor-pointer items-center justify-center rounded-full hover:bg-primary/20"
    >
      <HugeiconsIcon icon={icon} size={12} />
    </button>
  );

  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full border text-xs font-medium",
        active
          ? "border-primary/30 bg-primary/10 text-primary"
          : "border-dashed border-muted-foreground/40 text-muted-foreground"
      )}
    >
      {/* The arrows flank the label so the pill reads as a pager; they are siblings of the trigger, never
          nested in it, and paging applies live without opening the popover below. */}
      {stepping && (
        <span className="pl-1">
          {stepButton(-1, ArrowLeft01Icon, stepPreviousLabel)}
        </span>
      )}
      <Popover open={open} onOpenChange={onOpenChange}>
        {/* Wrapping the trigger, not wrapped by it — `asChild` has to reach a DOM element. */}
        <HintTooltip text={tooltip}>
          <PopoverTrigger asChild>
            <button
              type="button"
              aria-label={t("editEntry", { arg0: label })}
              className="max-w-64 cursor-pointer truncate rounded-full px-2.5 py-0.5"
            >
              {label}
              {text && `: ${text}`}
            </button>
          </PopoverTrigger>
        </HintTooltip>
        <PopoverContent
          align="start"
          className={cn("relative w-72 space-y-2 p-3", contentClassName)}
          // Radix would focus the first tabbable child on open, whatever the field asked for. Which
          // field takes the cursor — if any — is the field's decision: it is the one that knows that
          // focusing a [DateInput] opens a calendar over the rest of this popover ([RangeField] opts
          // out). The fields carry `autoFocus` themselves, so overriding this loses nothing.
          //
          // The popover *itself* takes it instead of nothing at all: with the focus left outside, the
          // trigger keeps it, and every re-render of the pill's draft then moves the focused element —
          // which makes the buttons in here unclickable (Playwright: "element is not stable"), and by
          // keyboard the popover would not be where Tab and Escape go.
          onOpenAutoFocus={(event) => {
            event.preventDefault();
            (event.currentTarget as HTMLElement | null)?.focus();
          }}
        >
          {/* A close cross top-right, on every filter's popover: edits apply live, so closing simply
              leaves the panel with what is applied (unlike "Abbrechen", which restores what it opened
              with). Room is kept for it with the label's own padding, so it never sits on the content. */}
          <button
            type="button"
            onClick={() => onOpenChange(false)}
            aria-label={t("close")}
            className="absolute right-2 top-2 flex size-5 cursor-pointer items-center justify-center rounded-full text-muted-foreground hover:bg-primary/20 hover:text-foreground"
          >
            <HugeiconsIcon icon={Cancel01Icon} size={12} />
          </button>
          <div className="pr-6">{children}</div>
          <div className="flex justify-end gap-1">
            <Button
              variant="ghost"
              size="sm"
              className="h-7 text-xs"
              onClick={onDelete}
            >
              {tAction("delete")}
            </Button>
            <Button
              variant="secondary"
              size="sm"
              className="h-7 text-xs"
              onClick={onCancel}
            >
              {tAction("cancel")}
            </Button>
          </div>
        </PopoverContent>
      </Popover>
      {stepping && (
        <span className={cn(!removable && "pr-1")}>
          {stepButton(1, ArrowRight01Icon, stepNextLabel)}
        </span>
      )}
      {removable && (
        <button
          type="button"
          onClick={onDelete}
          aria-label={t("removeEntry", { arg0: label })}
          className="mr-1.5 flex size-4 shrink-0 cursor-pointer items-center justify-center rounded-full hover:bg-primary/20"
        >
          <HugeiconsIcon icon={Cancel01Icon} size={10} />
        </button>
      )}
    </span>
  );
}
