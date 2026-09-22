"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Cancel01Icon } from "@hugeicons/core-free-icons";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { useDebouncedApply } from "@/components/data-table/use-debounced-apply";
import { cn } from "@/lib/utils";
import type { StyledTeamCalendar } from "@/lib/rs/calendar-types";
import { CalendarStylePopover } from "./calendar-style-popover";

interface CalendarPillProps {
  calendar: StyledTeamCalendar;
  onSetVisibility: (calendarId: number, visible: boolean) => void;
  onChangeStyle: (calendarId: number, bgColor: string | undefined) => void;
  onRemove: (calendarId: number) => void;
}

/**
 * One chosen calendar in the select's trigger row. Its coloured body opens the style popover
 * (visibility + colour), the × removes it from the selection. A hidden calendar is struck through, the
 * way the legacy react-select override marked it. A chosen colour takes effect at once — the popover
 * stays open — debounced only so a drag across the native picker's slider settles into one
 * `changeStyle` rather than one call per intermediate shade.
 */
export function CalendarPill({
  calendar,
  onSetVisibility,
  onChangeStyle,
  onRemove,
}: CalendarPillProps) {
  const t = useTranslations();
  const id = calendar.id;
  const title = calendar.title ?? "";
  const bgColor = calendar.style?.bgColor ?? "";
  // The colour the popover is showing; applied live once it settles (see below).
  const [pendingColor, setPendingColor] = useState(bgColor);

  // Commit the chosen colour as soon as it settles, comparing against the committed `bgColor` so our
  // own applied colour coming back does not re-fire and an external change wins over a stale draft.
  useDebouncedApply(pendingColor, bgColor, (color) => {
    if (id != null && color) onChangeStyle(id, color);
  });

  if (id == null) return null;

  // Start each opening from the calendar's current colour, in case it changed underneath us.
  const onOpenChange = (open: boolean) => {
    if (open) setPendingColor(bgColor);
  };

  return (
    <span className="inline-flex h-6 max-w-full items-center gap-1 rounded-full border border-border pr-1 pl-2 text-xs">
      <Popover onOpenChange={onOpenChange}>
        <PopoverTrigger asChild>
          <button
            type="button"
            aria-label={title}
            className={cn(
              "flex min-w-0 cursor-pointer items-center gap-1.5",
              !calendar.visible && "italic line-through opacity-50"
            )}
          >
            <span
              className="size-2.5 shrink-0 rounded-full border border-border/50"
              style={{ background: bgColor || "transparent" }}
              aria-hidden
            />
            <span className="truncate">{title}</span>
          </button>
        </PopoverTrigger>
        <PopoverContent align="start" className="w-auto">
          <CalendarStylePopover
            title={title}
            visible={calendar.visible}
            onToggleVisible={(visible) => onSetVisibility(id, visible)}
            color={pendingColor}
            onColorChange={setPendingColor}
          />
        </PopoverContent>
      </Popover>
      <button
        type="button"
        onClick={() => onRemove(id)}
        aria-label={`${t("delete")}: ${title}`}
        className="cursor-pointer opacity-60 hover:opacity-100"
      >
        <HugeiconsIcon icon={Cancel01Icon} size={12} />
      </button>
    </span>
  );
}
