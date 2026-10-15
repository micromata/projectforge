"use client";

import { HugeiconsIcon, type IconSvgElement } from "@hugeicons/react";
import { DropdownMenuItem } from "@/components/ui/dropdown-menu";

/**
 * A menu entry that says what it does, and below it what that means — the gear menu's entries (see
 * [GearMenuItem]) and the exports of a list toolbar (see [ExportMenu]).
 *
 * The explanation stands in the entry instead of in a tooltip: a tooltip inside a dropdown competes with
 * the menu for hover and focus, and these entries do something that is worth reading about *before*
 * clicking.
 */
export function DescribedMenuItem({
  label,
  description,
  icon,
  disabled,
  onSelect,
}: {
  label: string;
  description?: string;
  icon?: IconSvgElement;
  disabled?: boolean;
  onSelect: () => void;
}) {
  return (
    <DropdownMenuItem
      disabled={disabled}
      onSelect={onSelect}
      className="flex-col items-start gap-0.5"
    >
      <span className="flex items-center gap-1.5">
        {icon && <HugeiconsIcon icon={icon} size={14} aria-hidden />}
        {label}
      </span>
      {/* `whitespace-normal`: the menu primitive keeps its items on one line. */}
      {description && (
        <span className="text-[11px] whitespace-normal text-muted-foreground">
          {description}
        </span>
      )}
    </DropdownMenuItem>
  );
}
