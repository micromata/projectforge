"use client";

import type { ReactNode } from "react";
import { HugeiconsIcon, type IconSvgElement } from "@hugeicons/react";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Toggle } from "@/components/ui/toggle";

/** The buttons of [RichTextToolbar]: a toggle and a menu, both icon-only with their label as tooltip. */

export function ToolbarToggle({
  label,
  pressed,
  onPressedChange,
  children,
}: {
  label: string;
  pressed: boolean | undefined;
  onPressedChange: () => void;
  children: ReactNode;
}) {
  return (
    <HintTooltip title={label}>
      <Toggle
        size="sm"
        aria-label={label}
        pressed={pressed ?? false}
        onPressedChange={onPressedChange}
        // Keeps the editor's selection: a click on the toolbar must not blur it first.
        onMouseDown={(e) => e.preventDefault()}
      >
        {children}
      </Toggle>
    </HintTooltip>
  );
}

export function ToolbarMenu({
  label,
  icon,
  pressed,
  children,
}: {
  label: string;
  icon: IconSvgElement;
  pressed?: boolean;
  children: ReactNode;
}) {
  return (
    <DropdownMenu>
      <HintTooltip title={label}>
        <DropdownMenuTrigger asChild>
          <Toggle size="sm" aria-label={label} pressed={pressed ?? false}>
            <HugeiconsIcon icon={icon} />
          </Toggle>
        </DropdownMenuTrigger>
      </HintTooltip>
      <DropdownMenuContent align="start">{children}</DropdownMenuContent>
    </DropdownMenu>
  );
}
