"use client";

import { useFormatContext } from "@/hooks/use-format";
import { cn } from "@/lib/utils";
import {
  DEFAULT_FROM_TIME,
  zonedIsoOf,
  zonedPartsOf,
  type ZonedParts,
} from "@/lib/user-zone";
import { DateInput } from "./date-input";
import { TimeInput } from "./time-input";

export interface DateTimeInputProps {
  /** The instant as an ISO string in UTC — the wire format (see lib/user-zone.ts). */
  value: string | null | undefined;
  onChange: (value: string | null) => void;
  /** Accessible names for the two halves; they have no visible label of their own. */
  dateLabel: string;
  timeLabel: string;
  /** Time of day for a date entered without one: midnight for a start, 23:59 for an end. */
  fallbackTime?: string;
  autoFocus?: boolean;
  className?: string;
  /** Enter; passes the value that is in effect afterwards, as [DateInput] does. */
  onSubmit?: (value: string | null) => void;
}

/**
 * One point in time, entered as a date plus a time of day.
 *
 * The date half is [DateInput] and the time half [TimeInput], so the date layout, the calendar, the
 * first day of the week and the 12h/24h notation are all the account's — not the platform's.
 *
 * The wall clock is read in `userData.timeZone`, not the browser's zone, and handed on as UTC. Both
 * matter: the backend takes a zone-less timestamp for UTC, and the account's zone often differs
 * from the machine's.
 */
export function DateTimeInput({
  value,
  onChange,
  dateLabel,
  timeLabel,
  fallbackTime = DEFAULT_FROM_TIME,
  autoFocus,
  className,
  onSubmit,
}: DateTimeInputProps) {
  const ctx = useFormatContext();
  const parts = zonedPartsOf(value, ctx);

  /** The instant the two halves stand for, or null once the date is gone. */
  function isoOf(next: Partial<ZonedParts>): string | null {
    const date = next.date ?? parts?.date;
    const time = next.time ?? parts?.time;
    return zonedIsoOf(date, time, ctx, fallbackTime);
  }

  return (
    <div className={cn("flex items-start gap-1", className)}>
      <DateInput
        // The date takes the room that is left: its text is the longer of the two and the clear
        // button sits inside the field, so a cramped one truncates ("6.07.202").
        className="min-w-0 flex-1"
        aria-label={dateLabel}
        autoFocus={autoFocus}
        value={parts?.date ?? null}
        onChange={(date) => onChange(date ? isoOf({ date }) : null)}
        // The date [DateInput] just committed — `value` here is still the previous one.
        onSubmit={(date) => onSubmit?.(date ? isoOf({ date }) : null)}
      />
      <TimeInput
        aria-label={timeLabel}
        // Without a date there is no instant to attach a time to, so the field waits for one.
        disabled={!parts}
        value={parts?.time}
        onChange={(time) => onChange(isoOf({ time: time ?? fallbackTime }))}
        // The time [TimeInput] just committed — `value` here is still the previous one.
        onSubmit={(time) => onSubmit?.(isoOf({ time: time ?? fallbackTime }))}
        // Only as wide as its own text needs: "HH:mm" for an H24 account, and the extra room for a
        // day period only where there is one to show.
        className={cn("shrink-0", ctx.hour12 ? "w-[8.5rem]" : "w-[6rem]")}
      />
    </div>
  );
}
