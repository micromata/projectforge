"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Calendar01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { useFormatContext } from "@/hooks/use-format";
import { cn } from "@/lib/utils";
import { dateOf, todayIso } from "@/lib/date-parse";
import { MonthGrid } from "./month-grid";
import { useDatePickerLocale } from "./use-date-picker-locale";

/**
 * The calendar half of [DateInput]: the button that opens it, the month grid, and the shortcuts
 * below it. Split off only because the two together outgrow the file size this project allows.
 *
 * The week starts on the user's own first day (`weekStartsOn`), not the locale's default — that is
 * the setting this whole component exists for (see FormatContext in lib/format.ts).
 *
 * Open state belongs to the caller, because focusing the text field opens this too — see [DateInput].
 */
export function DateInputCalendar({
  value,
  defaultMonth,
  onChange,
  disabled,
  open,
  onOpenChange,
  onPicked,
  fieldRef,
  hiddenWhileFocused,
}: {
  value: string | null | undefined;
  /** Where an empty field opens the calendar, as `yyyy-MM-dd`; see [DateInput]. */
  defaultMonth?: string | null;
  onChange: (value: string | null) => void;
  disabled?: boolean;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** A day was chosen, so the field can move the focus on. */
  onPicked: () => void;
  /** The text field this belongs to; clicks and keys in it must not count as "outside". */
  fieldRef?: React.RefObject<HTMLInputElement | null>;
  /**
   * Whether the clear button takes this button's place while the field has the focus — see [DateInput],
   * which owns that rule. `visibility`, not unmounting: the hidden button is out of the tab order and
   * out of the accessibility tree, but keeps the box this popover is positioned against.
   */
  hiddenWhileFocused?: boolean;
}) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const pickerLocale = useDatePickerLocale();
  const [month, setMonth] = useState(
    () => dateOf(value) ?? dateOf(defaultMonth) ?? new Date()
  );

  // Follows the value while the popover is closed, so opening it lands on the month of the date in
  // the field rather than on wherever it was browsed to last. An empty field follows `defaultMonth`
  // instead — the other end of a period, which is only known once that one is filled in.
  const [synced, setSynced] = useState({ value, defaultMonth });
  if (synced.value !== value || synced.defaultMonth !== defaultMonth) {
    setSynced({ value, defaultMonth });
    const asDate = dateOf(value) ?? (value ? null : dateOf(defaultMonth));
    if (asDate) setMonth(asDate);
  }

  // The callbacks arrive as inline arrows from [DateInput], i.e. new on every render. Kept in a ref
  // and called through a stable wrapper, so [MonthGrid]'s memo actually holds — a changing `onPick`
  // would remount the grid on every render and bring the swallowed first click back (see there).
  const handlers = useRef({ onChange, onOpenChange, onPicked });
  // In an effect, not during render: a ref must not be written while rendering, and the calendar can
  // only be clicked once the render has committed anyway.
  useEffect(() => {
    handlers.current = { onChange, onOpenChange, onPicked };
  }, [onChange, onOpenChange, onPicked]);
  const close = useCallback((next: string | null | undefined) => {
    if (next !== undefined) handlers.current.onChange(next);
    handlers.current.onOpenChange(false);
    handlers.current.onPicked();
  }, []);

  return (
    <Popover open={open} onOpenChange={onOpenChange}>
      <PopoverTrigger asChild>
        <Button
          type="button"
          variant="ghost"
          size="icon"
          disabled={disabled}
          // Inside the text field, at its right edge — the field reserves the padding for it (see
          // DateInput). Not `size-7`: a button that tall would sit on the box's border.
          className={cn(
            "absolute inset-y-0.5 right-0.5 size-auto w-5 text-muted-foreground",
            hiddenWhileFocused && "group-focus-within:invisible"
          )}
          aria-label={t("calendar.chooseDate")}
        >
          <HugeiconsIcon icon={Calendar01Icon} size={14} />
        </Button>
      </PopoverTrigger>
      <PopoverContent
        align="start"
        className="w-auto p-0"
        // Named here rather than by its trigger, which is hidden while the field has the focus and the
        // clear button stands in its place (see [hiddenWhileFocused]).
        aria-label={t("calendar.chooseDate")}
        // The field keeps the focus it never gave up: Radix would hand it back to the trigger, and that
        // is the button which may be invisible at exactly this moment. [DateInput] refocuses the field
        // itself after a pick (`onPicked`).
        onCloseAutoFocus={(event) => event.preventDefault()}
        // The caret stays in the text field: this opens on focus, and pulling focus into the calendar
        // would make typing impossible.
        onOpenAutoFocus={(event) => event.preventDefault()}
        // Typing and clicking in the field it belongs to are not "outside" — otherwise the calendar
        // would close on the first keystroke after it opened itself.
        onInteractOutside={(event) => {
          if (fieldRef?.current?.contains(event.target as Node)) {
            event.preventDefault();
          }
        }}
      >
        <MonthGrid
          value={value}
          month={month}
          onMonthChange={setMonth}
          onPick={close}
          locale={pickerLocale}
          weekStartsOn={ctx.weekStartsOn}
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
