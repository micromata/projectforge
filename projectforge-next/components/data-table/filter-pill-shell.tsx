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
import { FilterPillFace, filterPillFrameClass } from "./filter-pill-face";

interface FilterPillShellProps {
  label: string;
  /** The value as text, appended after the label: "Modified: Kai Reinhard, …". */
  text?: string;
  /** Picks left off [text], counted beside it (see [filterPillContent]). */
  more?: number;
  tooltip?: string;
  /** Show [tooltip] verbatim, line by line, instead of as markdown — for a list of user data. */
  tooltipPlain?: boolean;
  /** Filled pills read as solid, empty ones as a dashed outline. */
  active: boolean;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** Default filters stay on the row, so they only offer emptying, not removing. */
  removable: boolean;
  /** Restores the value the popover opened with and closes. */
  onCancel: () => void;
  /**
   * "Übernehmen": applies the draft at once (without waiting for the debounce) and closes. Edits apply
   * live anyway, so this is the explicit "done", the counterpart of "Abbrechen".
   */
  onApply: () => void;
  onDelete: () => void;
  /** Wider than the default for a pill holding more than one field. */
  contentClassName?: string;
  /**
   * A pill with a single on/off state (a BOOLEAN filter): the trigger toggles the value in place
   * instead of opening a popover, so there is no checkbox step. When set, the popover, its children
   * and the delete/cancel/apply footer are not rendered — the trailing remove X (for a non-default filter)
   * and any step arrows stay as they are. See [FilterPill].
   */
  onToggle?: () => void;
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
 * delete/cancel/apply footer.
 *
 * Shared so that a pill standing for one backend field ([FilterPill]) and the one standing for the
 * three grouped history fields ([HistoryFilterPill]) are the same thing on screen and by keyboard —
 * only their contents and what they apply differ.
 */
export function FilterPillShell({
  label,
  text,
  more,
  tooltip,
  tooltipPlain,
  active,
  open,
  onOpenChange,
  removable,
  onCancel,
  onApply,
  onDelete,
  contentClassName,
  onStep,
  stepPreviousLabel,
  stepNextLabel,
  onToggle,
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
    <span className={filterPillFrameClass(active)}>
      {/* The arrows flank the label so the pill reads as a pager; they are siblings of the trigger, never
          nested in it, and paging applies live without opening the popover below. */}
      {stepping && (
        <span className="pl-1">
          {stepButton(-1, ArrowLeft01Icon, stepPreviousLabel)}
        </span>
      )}
      {onToggle ? (
        // A single on/off filter: the trigger flips the value in place — no popover, no checkbox.
        <HintTooltip text={tooltip} plain={tooltipPlain}>
          <button
            type="button"
            aria-label={label}
            aria-pressed={active}
            onClick={onToggle}
            className="min-w-0 cursor-pointer rounded-full px-2.5 py-0.5"
          >
            <FilterPillFace label={label} text={text} more={more} />
          </button>
        </HintTooltip>
      ) : (
        <Popover open={open} onOpenChange={onOpenChange}>
          {/* Wrapping the trigger, not wrapped by it — `asChild` has to reach a DOM element. */}
          <HintTooltip text={tooltip} plain={tooltipPlain}>
            <PopoverTrigger asChild>
              <button
                type="button"
                aria-label={t("editEntry", { arg0: label })}
                className="min-w-0 cursor-pointer rounded-full px-2.5 py-0.5"
              >
                <FilterPillFace label={label} text={text} more={more} />
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
            {/* Dialog order: the destructive "Löschen" apart on the left, "Abbrechen" right before the
                primary "Übernehmen". */}
            <div className="flex gap-1">
              <Button
                variant="ghost"
                size="sm"
                className="h-7 text-xs"
                onClick={onDelete}
              >
                {tAction("delete")}
              </Button>
              <div className="flex-1" />
              <Button
                variant="outline"
                size="sm"
                className="h-7 text-xs"
                onClick={onCancel}
              >
                {tAction("cancel")}
              </Button>
              <Button size="sm" className="h-7 text-xs" onClick={onApply}>
                {tAction("apply")}
              </Button>
            </div>
          </PopoverContent>
        </Popover>
      )}
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
