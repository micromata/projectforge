"use client";

import { useId } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { fetchTeamCalendars } from "@/lib/rs/team-event";

interface Props {
  value: number | null;
  onChange: (teamCalId: number) => void;
  disabled?: boolean;
}

/**
 * The calendar the imported events go into: the user's writable calendars (full access, no external
 * subscription) — the same list the event editor offers, which the backend checks again on every call.
 */
export function CalendarTargetSelect({ value, onChange, disabled }: Props) {
  const t = useTranslations();
  const id = useId();
  const calendars = useQuery({
    queryKey: ["teamEvent", "calendars"],
    queryFn: ({ signal }) => fetchTeamCalendars(signal),
  });

  return (
    <div className="flex max-w-sm flex-col gap-1.5">
      <Label htmlFor={id} className="text-sm">
        {t("plugins.teamcal.calendar")}
      </Label>
      <Select
        value={value != null ? String(value) : ""}
        onValueChange={(selected) => onChange(Number(selected))}
        disabled={disabled || calendars.isPending}
      >
        <SelectTrigger id={id} className="h-8">
          <SelectValue placeholder={t("select._")} />
        </SelectTrigger>
        <SelectContent>
          {(calendars.data ?? []).map((calendar) => (
            <SelectItem key={calendar.id} value={String(calendar.id)}>
              {calendar.title}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  );
}
