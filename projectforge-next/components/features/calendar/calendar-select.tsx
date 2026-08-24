"use client";

import { useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Calendar03Icon, Tick02Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from "@/components/ui/command";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { useFormatContext } from "@/hooks/use-format";
import { cn } from "@/lib/utils";
import type { StyledTeamCalendar } from "@/lib/rs/calendar-types";
import { CalendarPill } from "./calendar-pill";

interface CalendarSelectProps {
  teamCalendars: StyledTeamCalendar[];
  activeCalendars: StyledTeamCalendar[];
  onSetActive: (calendars: StyledTeamCalendar[]) => void;
  onSetVisibility: (calendarId: number, visible: boolean) => void;
  onChangeStyle: (calendarId: number, bgColor: string | undefined) => void;
}

/**
 * The calendar chooser: the chosen calendars as pills, and a searchable command list of all of them to
 * add or drop one. Replaces the legacy react-select `isMulti`. The menu stays open while several are
 * ticked, and the pills are sorted by title the way the backend sorts the list — stable after a local
 * add so a pill does not jump.
 */
export function CalendarSelect({
  teamCalendars,
  activeCalendars,
  onSetActive,
  onSetVisibility,
  onChangeStyle,
}: CalendarSelectProps) {
  const t = useTranslations();
  const { locale } = useFormatContext();
  const [open, setOpen] = useState(false);

  const activeIds = useMemo(
    () => new Set(activeCalendars.map((c) => c.id)),
    [activeCalendars]
  );
  const sortedActive = useMemo(
    () =>
      [...activeCalendars].sort((a, b) =>
        (a.title ?? "").localeCompare(b.title ?? "", locale)
      ),
    [activeCalendars, locale]
  );

  const toggle = (calendar: StyledTeamCalendar) => {
    if (activeIds.has(calendar.id))
      onSetActive(activeCalendars.filter((c) => c.id !== calendar.id));
    else onSetActive([...activeCalendars, calendar]);
  };

  return (
    <div className="flex min-w-0 flex-wrap items-center gap-1.5">
      {sortedActive.map((calendar) => (
        <CalendarPill
          key={calendar.id}
          calendar={calendar}
          onSetVisibility={onSetVisibility}
          onChangeStyle={onChangeStyle}
          onRemove={(id) =>
            onSetActive(activeCalendars.filter((c) => c.id !== id))
          }
        />
      ))}
      <Popover open={open} onOpenChange={setOpen}>
        <PopoverTrigger asChild>
          <Button
            type="button"
            variant="outline"
            size="sm"
            role="combobox"
            aria-expanded={open}
            aria-label={t("calendar.title")}
            className="h-7 gap-1.5"
          >
            <HugeiconsIcon icon={Calendar03Icon} size={14} aria-hidden />
            {activeCalendars.length}
          </Button>
        </PopoverTrigger>
        <PopoverContent align="start" className="w-64 p-0">
          <Command>
            <CommandInput placeholder={t("search")} />
            <CommandList>
              <CommandEmpty>{t("nothingFound")}</CommandEmpty>
              <CommandGroup>
                {teamCalendars.map((calendar) => (
                  <CommandItem
                    key={calendar.id}
                    value={calendar.title ?? String(calendar.id)}
                    onSelect={() => toggle(calendar)}
                  >
                    <span
                      className="size-2.5 shrink-0 rounded-full border border-border/50"
                      style={{
                        background: calendar.style?.bgColor || "transparent",
                      }}
                      aria-hidden
                    />
                    <span className="flex-1 truncate">{calendar.title}</span>
                    {activeIds.has(calendar.id) && (
                      <HugeiconsIcon
                        icon={Tick02Icon}
                        size={14}
                        className={cn("text-primary")}
                        aria-hidden
                      />
                    )}
                  </CommandItem>
                ))}
              </CommandGroup>
            </CommandList>
          </Command>
        </PopoverContent>
      </Popover>
    </div>
  );
}
