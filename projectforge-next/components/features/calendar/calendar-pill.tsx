"use client";

import { useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Cancel01Icon } from "@hugeicons/core-free-icons";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
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
 * way the legacy react-select override marked it. The colour is committed once on close, so dragging
 * across the palette is a single `changeStyle`.
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
  // The colour the popover is showing; committed to the backend only when it closes.
  const [pendingColor, setPendingColor] = useState(bgColor);
  const committed = useRef(bgColor);

  if (id == null) return null;

  const onOpenChange = (open: boolean) => {
    if (open) {
      setPendingColor(bgColor);
      committed.current = bgColor;
      return;
    }
    if (pendingColor && pendingColor !== committed.current) {
      committed.current = pendingColor;
      onChangeStyle(id, pendingColor);
    }
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
