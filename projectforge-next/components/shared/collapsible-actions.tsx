"use client";

import { useEffect, useRef, useState, type ReactNode } from "react";
import { useTranslations } from "next-intl";
import { MoreHorizontalIcon } from "@hugeicons/core-free-icons";
import { cn } from "@/lib/utils";
import { CrampedToggleButton } from "@/components/shared/list/cramped-toggle-button";

interface CollapsibleActionsProps {
  children: ReactNode;
  /** Marked as on although collapsed: something in the collected actions is active (the selection mode). */
  highlighted?: boolean;
}

/**
 * The secondary actions of a page head. Beside each other while the head is wide; below 72rem (a narrow window, a
 * tablet in portrait, a phone) they are collected behind one "more" button and shown in a panel under it, over the
 * page rather than in a line of their own, so the head never grows by them. Wherever the head is, an ancestor must
 * be a `@container` (ListToolbar, PageTitleRow).
 *
 * The panel stays mounted when closed, so an action's dialog that is open stays too. A tap outside it or Escape
 * closes it.
 */
export function CollapsibleActions({
  children,
  highlighted = false,
}: CollapsibleActionsProps) {
  const t = useTranslations();
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (!open) return;
    const close = (e: Event) => {
      if (e instanceof KeyboardEvent) {
        if (e.key === "Escape") setOpen(false);
      } else if (!ref.current?.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener("pointerdown", close);
    document.addEventListener("keydown", close);
    return () => {
      document.removeEventListener("pointerdown", close);
      document.removeEventListener("keydown", close);
    };
  }, [open]);
  return (
    <div ref={ref} className="relative flex items-center">
      <div
        // The page's table lifts its head above the sticky column header while this is set (see DataTable).
        data-actions-open={open || undefined}
        className={cn(
          "flex items-center gap-2 sm:gap-3",
          "@max-6xl:absolute @max-6xl:top-full @max-6xl:right-0 @max-6xl:z-30 @max-6xl:mt-1 @max-6xl:flex-col @max-6xl:items-stretch @max-6xl:gap-1 @max-6xl:rounded-lg @max-6xl:border @max-6xl:bg-popover @max-6xl:p-2 @max-6xl:shadow-lg",
          !open && "@max-6xl:hidden"
        )}
      >
        {children}
      </div>
      <CrampedToggleButton
        icon={MoreHorizontalIcon}
        label={t("more")}
        iconOnly
        open={open}
        onToggle={() => setOpen((o) => !o)}
        highlighted={highlighted && !open}
        showWhen="narrow"
      />
    </div>
  );
}
