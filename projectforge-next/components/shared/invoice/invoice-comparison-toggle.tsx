"use client";

import type { KeyboardEvent, ReactNode } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowDown01Icon, ArrowRight01Icon } from "@hugeicons/core-free-icons";
import { cn } from "@/lib/utils";

/**
 * The caret of the invoice statistics: the arrow that points right when the year-earlier figures are
 * collapsed and down when they are open. It is only the affordance now — a decorative icon, not a
 * control: [ComparisonToggleRegion] carries the click, so the whole line (and the whole comparison
 * table) toggles, not just this 16px target.
 */
export function ComparisonCaret({
  expanded,
  canCompare,
}: {
  expanded: boolean;
  canCompare: boolean;
}) {
  return (
    <span
      aria-hidden
      className={cn(
        "-my-0.5 inline-flex items-center align-middle",
        canCompare ? "opacity-70" : "opacity-30"
      )}
    >
      <HugeiconsIcon
        icon={expanded ? ArrowDown01Icon : ArrowRight01Icon}
        size={16}
      />
    </span>
  );
}

/**
 * The region that toggles the previous-year comparison: it wraps the compact statistics line when
 * collapsed and the whole comparison table when open, so a click anywhere on either — not only on the
 * caret — expands or collapses the figures a year earlier (see [ComparisonCaret]).
 *
 * A `<dl>`/`<table>` cannot live inside a `<button>`, so this is the disclosure pattern on a
 * `role="button"` element with `Enter`/`Space` support rather than a native button.
 *
 * The comparison needs a bounded Rechnungsdatum range to have a period at all; without one (`!canCompare`)
 * the region is inert and its tooltip says why. With no `onToggle` at all (the mass-update summary has no
 * toggle) the children are rendered as they are, with no interactivity.
 */
export function ComparisonToggleRegion({
  expanded,
  canCompare,
  onToggle,
  children,
}: {
  expanded: boolean;
  canCompare: boolean;
  onToggle?: (on: boolean) => void;
  children: ReactNode;
}) {
  const t = useTranslations();

  if (!onToggle) return <>{children}</>;

  const label = t("fibu.rechnung.statistics.previousYearComparison");
  const toggle = () => onToggle(!expanded);
  const onKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      toggle();
    }
  };

  return (
    <div
      role="button"
      aria-expanded={expanded}
      aria-label={label}
      aria-disabled={!canCompare}
      tabIndex={canCompare ? 0 : undefined}
      onClick={canCompare ? toggle : undefined}
      onKeyDown={canCompare ? onKeyDown : undefined}
      title={
        canCompare
          ? label
          : t("fibu.rechnung.statistics.previousYearComparisonHint")
      }
      className={cn(
        canCompare
          ? "cursor-pointer transition-colors hover:bg-muted/60"
          : "cursor-default"
      )}
    >
      {children}
    </div>
  );
}
