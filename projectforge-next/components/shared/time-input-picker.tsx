"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Clock01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { useFormatContext } from "@/hooks/use-format";
import { hourLabelOf } from "@/lib/time-parse";
import { cn } from "@/lib/utils";

/** Minutes between the entries of the minute column, as the legacy TimeInput's `precision`. */
const MINUTE_STEP = 5;

const MORNING = Array.from({ length: 12 }, (_, hour) => hour);
const AFTERNOON = MORNING.map((hour) => hour + 12);
const MINUTES = Array.from(
  { length: 60 / MINUTE_STEP },
  (_, index) => index * MINUTE_STEP
);

/**
 * The picker half of [TimeInput]: two hour columns and a minute column, as the legacy
 * `TimeInput.jsx` had them — the fast way to set a time without typing, which a `<input type="time">`
 * gives for free and a text field does not.
 *
 * Splitting the hours 0-11 / 12-23 keeps the whole day reachable in two short columns. An H12 account
 * sees the same two labelled "1 vorm." … "12 nachm." (see [hourLabelOf]), so the notation follows the
 * account here as well.
 *
 * A pick leaves the popover open, as the legacy one did: setting a time usually means picking an hour
 * *and* a minute, and closing after the first would make the second a second trip.
 *
 * Open state belongs to the caller, because focusing the text field opens this too — see [TimeInput].
 */
export function TimeInputPicker({
  hour,
  minute,
  onChange,
  disabled,
  open,
  onOpenChange,
  fieldRef,
}: {
  /** The 24h hour and the minute currently in the field, or null when it is empty. */
  hour: number | null;
  minute: number | null;
  onChange: (hour: number, minute: number) => void;
  disabled?: boolean;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** The text field this belongs to; clicks and keys in it must not count as "outside". */
  fieldRef?: React.RefObject<HTMLInputElement | null>;
}) {
  const t = useTranslations();
  const ctx = useFormatContext();

  return (
    <Popover open={open} onOpenChange={onOpenChange}>
      <PopoverTrigger asChild>
        <Button
          type="button"
          variant="ghost"
          size="icon"
          disabled={disabled}
          className="size-7 shrink-0 text-muted-foreground"
          aria-label={t("calendar.chooseTime")}
        >
          <HugeiconsIcon icon={Clock01Icon} size={14} />
        </Button>
      </PopoverTrigger>
      <PopoverContent
        align="start"
        // flex-row, because PopoverContent stacks its children by default.
        className="flex w-auto flex-row gap-0 p-1"
        // The caret stays in the text field: this opens on focus, and pulling focus into the popover
        // would make typing impossible.
        onOpenAutoFocus={(event) => event.preventDefault()}
        // Typing and clicking in the field it belongs to are not "outside" — otherwise the popover
        // would close on the first keystroke after it opened itself.
        onInteractOutside={(event) => {
          if (fieldRef?.current?.contains(event.target as Node)) {
            event.preventDefault();
          }
        }}
      >
        {[MORNING, AFTERNOON].map((column, index) => (
          <ul key={index} className="max-h-64 overflow-y-auto">
            {column.map((value) => (
              <Unit
                key={value}
                label={hourLabelOf(value, ctx)}
                selected={value === hour}
                // An hour picked before a minute is one means the top of the hour.
                onClick={() => onChange(value, minute ?? 0)}
              />
            ))}
          </ul>
        ))}
        <ul className="max-h-64 overflow-y-auto border-l">
          {MINUTES.map((value) => (
            <Unit
              key={value}
              label={String(value).padStart(2, "0")}
              // Rounded down to the step, so a typed 13:47 marks 45 rather than nothing at all.
              selected={
                minute != null && minute - (minute % MINUTE_STEP) === value
              }
              onClick={() => onChange(hour ?? 0, value)}
            />
          ))}
        </ul>
      </PopoverContent>
    </Popover>
  );
}

function Unit({
  label,
  selected,
  onClick,
}: {
  label: string;
  selected: boolean;
  onClick: () => void;
}) {
  return (
    <li>
      <button
        type="button"
        onClick={onClick}
        aria-current={selected || undefined}
        className={cn(
          "w-full cursor-pointer rounded-sm px-2 py-0.5 text-center text-xs tabular-nums hover:bg-accent",
          selected && "bg-primary font-medium text-primary-foreground"
        )}
      >
        {label}
      </button>
    </li>
  );
}
