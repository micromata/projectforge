"use client";

import { HugeiconsIcon, type IconSvgElement } from "@hugeicons/react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

interface CrampedToggleButtonProps {
  icon: IconSvgElement;
  /** The text beside the icon, or its accessible name only with [iconOnly]. */
  label: string;
  iconOnly?: boolean;
  open: boolean;
  onToggle: () => void;
  /** A count beside the label (the active filters), none for 0. */
  count?: number;
  /** Marked as on although folded: something in the folded part is active (the selection mode). */
  highlighted?: boolean;
  /**
   * When the button is shown: on a `cramped` screen (the default), or whenever the head is narrow (below 72rem wide,
   * measured on the toolbar, see ListToolbar) — a tablet in portrait or a narrow window as well.
   */
  showWhen?: "cramped" | "narrow";
}

/**
 * Folds a part of the list's head in and out on a cramped screen (the actions, the filter row) — only there: it
 * isn't rendered anywhere else (`cramped:inline-flex`), where that part is always shown.
 */
export function CrampedToggleButton({
  icon,
  label,
  iconOnly = false,
  open,
  onToggle,
  count = 0,
  highlighted = false,
  showWhen = "cramped",
}: CrampedToggleButtonProps) {
  return (
    <Button
      type="button"
      variant={highlighted ? "default" : open ? "secondary" : "ghost"}
      size={iconOnly ? "icon" : "sm"}
      className={cn(
        "hidden gap-1.5",
        showWhen === "narrow" ? "@max-6xl:inline-flex" : "cramped:inline-flex",
        iconOnly && "size-8"
      )}
      aria-expanded={open}
      aria-label={iconOnly ? label : undefined}
      onClick={onToggle}
    >
      <HugeiconsIcon icon={icon} size={16} aria-hidden />
      {!iconOnly && label}
      {count > 0 && (
        <span className="rounded-full bg-primary px-1.5 text-[11px] leading-4 text-primary-foreground tabular-nums">
          {count}
        </span>
      )}
    </Button>
  );
}
