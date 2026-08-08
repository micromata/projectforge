"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Calendar01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { useFormatContext } from "@/hooks/use-format";
import { dateOf, isoOf, todayIso } from "@/lib/date-parse";
import { useDatePickerLocale } from "./use-date-picker-locale";

/**
 * The calendar half of [DateInput]: the button that opens it, the month grid, and the shortcuts
 * below it. Split off only because the two together outgrow the file size this project allows.
 *
 * The week starts on the user's own first day (`weekStartsOn`), not the locale's default — that is
 * the setting this whole component exists for (see FormatContext in lib/format.ts).
 */
export function DateInputCalendar({
  value,
  onChange,
  disabled,
  onClosed,
}: {
  value: string | null | undefined;
  onChange: (value: string | null) => void;
  disabled?: boolean;
  /** Puts the focus back into the text field, wherever the popover was left from. */
  onClosed: () => void;
}) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const pickerLocale = useDatePickerLocale();
  const [open, setOpen] = useState(false);
  const [month, setMonth] = useState(() => dateOf(value) ?? new Date());

  // Follows the value while the popover is closed, so opening it lands on the month of the date in
  // the field rather than on wherever it was browsed to last.
  const [synced, setSynced] = useState(value);
  if (synced !== value) {
    setSynced(value);
    const asDate = dateOf(value);
    if (asDate) setMonth(asDate);
  }

  function close(next: string | null | undefined) {
    if (next !== undefined) onChange(next);
    setOpen(false);
    onClosed();
  }

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          type="button"
          variant="ghost"
          size="icon"
          disabled={disabled}
          className="size-7 shrink-0 text-muted-foreground"
          aria-label={t("calendar.chooseDate")}
        >
          <HugeiconsIcon icon={Calendar01Icon} size={14} />
        </Button>
      </PopoverTrigger>
      <PopoverContent align="start" className="w-auto p-0">
        <Calendar
          mode="single"
          selected={dateOf(value) ?? undefined}
          month={month}
          onMonthChange={setMonth}
          // Clicking the selected day again clears it, as in the legacy picker.
          onSelect={(date) => close(date ? isoOf(date) : null)}
          locale={pickerLocale}
          weekStartsOn={ctx.weekStartsOn}
          captionLayout="dropdown"
          autoFocus
          // Today only gets a grey background from the primitive, which is hard to tell from a
          // hovered day. A ring in the accent colour reads as "here you are" even when another day
          // is selected; on the selected day itself it sits inside its filled button.
          classNames={{
            today:
              "rounded-(--cell-radius) font-semibold text-primary ring-1 ring-inset ring-primary/70",
          }}
        />
        <div className="flex gap-1 border-t p-2">
          <Button
            type="button"
            variant="ghost"
            size="sm"
            className="flex-1 text-xs"
            onClick={() => close(todayIso())}
          >
            {t("calendar.today")}
          </Button>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            className="flex-1 text-xs"
            disabled={!value}
            onClick={() => close(null)}
          >
            {t("reset")}
          </Button>
        </div>
      </PopoverContent>
    </Popover>
  );
}
