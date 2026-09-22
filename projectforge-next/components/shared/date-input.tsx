"use client";

import { useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Cancel01Icon } from "@hugeicons/core-free-icons";
import { Input } from "@/components/ui/input";
import { useFormatContext } from "@/hooks/use-format";
import {
  formatDateInput,
  parseDateInput,
  shiftDateByDays,
} from "@/lib/date-parse";
import { cn } from "@/lib/utils";
import { DateInputCalendar } from "./date-input-calendar";

export interface DateInputProps {
  /** The date as `yyyy-MM-dd` — the wire format of a LocalDate (see lib/date-parse.ts). */
  value: string | null | undefined;
  onChange: (value: string | null) => void;
  /**
   * The month the calendar opens on while the field is empty, as `yyyy-MM-dd`. For the end of a
   * period: with a begin entered and no end yet, browsing starts at the begin's month instead of at
   * today's — the end is nearly always in or after it (see [DatePeriodField]).
   */
  defaultMonth?: string | null;
  id?: string;
  /** Accessible name, when no `<label htmlFor>` names the field. */
  "aria-label"?: string;
  className?: string;
  autoFocus?: boolean;
  disabled?: boolean;
  required?: boolean;
  invalid?: boolean;
  /**
   * Enter; the filter pill uses it to save and close its popover. The committed value is passed
   * along because `onChange` fires in the same handler, so the caller's state is still the old one.
   */
  onSubmit?: (value: string | null) => void;
  onBlur?: () => void;
}

/**
 * The one way a date is entered in this app: typed in the user's layout, or picked from a calendar
 * that starts the week on the user's first day.
 *
 * A native `<input type="date">` cannot do either — its layout and first day of week come from the
 * browser and operating system, so an English machine would show `08/09/2026` next to the
 * `09.08.2026` of the table beside it. Locale, first day of week and the placeholder mask all come
 * from `userData` through [useFormatContext], the same source the display side uses (lib/format.ts).
 *
 * Typing and committing follow the legacy DateInput.jsx: strict while typing, lenient on blur and
 * Enter, arrow keys step a day. See lib/date-parse.ts for why.
 */
export function DateInput({
  value,
  onChange,
  defaultMonth,
  id,
  className,
  autoFocus,
  disabled,
  required,
  invalid,
  onSubmit,
  onBlur,
  "aria-label": ariaLabel,
}: DateInputProps) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const [text, setText] = useState(() => formatDateInput(value, ctx));
  const inputRef = useRef<HTMLInputElement>(null);
  // Opened by focusing the field, which is what date inputs elsewhere do — the button beside it
  // stays, both for the mouse and as the thing that names the popover for a screen reader.
  const [pickerOpen, setPickerOpen] = useState(false);
  // Set while the focus is being put back after a pick, so the calendar does not reopen right after
  // closing itself.
  const refocusing = useRef(false);

  // The value can change without this field being touched: a form reset after the entity loaded, a
  // saved filter being applied, a calendar pick. The text follows it, but only when it means a
  // different day — otherwise a half-typed "9.8." would be rewritten while it is still being typed.
  // Adjusted during render rather than in an effect, which is what React prescribes for state that
  // derives from props (https://react.dev/learn/you-might-not-need-an-effect): no second render pass
  // showing the stale text, and no cascading render.
  const [synced, setSynced] = useState({ value, locale: ctx.locale });
  if (synced.value !== value || synced.locale !== ctx.locale) {
    setSynced({ value, locale: ctx.locale });
    if (parseDateInput(text, ctx) !== (value ?? null)) {
      setText(formatDateInput(value, ctx));
    }
  }

  /**
   * Reads the text and returns the value that is in effect afterwards.
   *
   * Only ever reports a value that differs from the current one. Blur commits too, and a blur happens
   * in the middle of clicking a day in the calendar: an `onChange` there re-renders the form, which
   * remounted the calendar between mousedown and mouseup — so the browser saw its two halves on
   * different nodes and fired no click at all. The first pick was swallowed, the second one worked.
   */
  function commit(raw: string): string | null {
    if (raw.trim() === "") {
      if (value != null) onChange(null);
      setText("");
      return null;
    }
    const parsed = parseDateInput(raw, ctx);
    // Not a date: keep the value and put its text back, rather than silently dropping what is there.
    setText(formatDateInput(parsed ?? value, ctx));
    if (parsed && parsed !== value) onChange(parsed);
    return parsed ?? value ?? null;
  }

  function clear() {
    setText("");
    onChange(null);
    inputRef.current?.focus();
  }

  return (
    <div
      className={cn(
        // A date is ten characters wide and never more, so the field stops growing once it fits one
        // plus the single button at its right edge instead of stretching over the whole column it sits
        // in. It still shrinks below that where the space is narrower — a filter popover, the two
        // halves of a DatePeriodField, where two of these plus the quick access share one column.
        "flex w-full max-w-28 items-center",
        className
      )}
    >
      {/* The button sits *inside* the box, as it does in the search inputs of this app: a date is a
          short value, and an icon parked beside its field made the whole thing wider than the ten
          characters it holds. `group`, because the two buttons take turns by focus (see below). */}
      <div className="group relative min-w-0 flex-1">
        <Input
          ref={inputRef}
          id={id}
          aria-label={ariaLabel}
          aria-invalid={invalid || undefined}
          autoFocus={autoFocus}
          // Never the field a form starts in, unless it is asked for explicitly: focusing this opens
          // the calendar, and a form that greets the user with a popover over its own fields is worse
          // than one whose cursor waits (see useFocusFirstField).
          data-autofocus={autoFocus ? undefined : "skip"}
          disabled={disabled}
          required={required}
          inputMode="numeric"
          // Text, not "date": the mask is the user's, not the browser's.
          type="text"
          placeholder={ctx.datePattern}
          value={text}
          // Room for one button, never two: the clear button shares the calendar's place rather than
          // taking a second one (see below). `tabular-nums` so a typed date is never wider than the
          // one this field is measured for.
          className="px-2.5 pr-7 tabular-nums"
          onFocus={() => {
            if (!refocusing.current) setPickerOpen(true);
            refocusing.current = false;
          }}
          // A click reopens it too, not only the focus: after a pick the field keeps the focus it never
          // gave up (see onPicked), so focusing it again fires nothing — and the calendar button that
          // would reopen it is hidden while the focus is here. Without this a picked date could not be
          // corrected with the mouse without leaving the field first. Harmless on the opening click,
          // where onFocus already set it; the programmatic refocus is a focus, not a click, so it never
          // reopens what a pick just closed.
          onClick={() => setPickerOpen(true)}
          onChange={(e) => {
            setText(e.target.value);
            // Strict, so a date is only committed once it is fully typed.
            const parsed = parseDateInput(e.target.value, ctx, {
              strict: true,
            });
            if (parsed) onChange(parsed);
            else if (e.target.value.trim() === "") onChange(null);
          }}
          onBlur={() => {
            commit(text);
            onBlur?.();
          }}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              e.preventDefault();
              setPickerOpen(false);
              onSubmit?.(commit(text));
              return;
            }
            // Deliberately no Escape-clears-the-field: this input sits inside popovers and dialogs
            // whose Escape closes them, and that binding wins (Radix listens on the document). The
            // clear button and the one in the calendar are the ways to empty it.
            if (e.key !== "ArrowUp" && e.key !== "ArrowDown") return;
            const base = parseDateInput(text, ctx) ?? value;
            const shifted = shiftDateByDays(base, e.key === "ArrowUp" ? 1 : -1);
            if (!shifted) return;
            e.preventDefault();
            onChange(shifted);
          }}
        />
        {text !== "" && !disabled && (
          // Inside the field, as the search inputs of this app clear themselves — a date is cleared
          // often enough (an optional deadline, a filter range) that selecting the text and deleting
          // it is a chore.
          //
          // In the calendar button's place, not beside it, and only while the field has the focus: two
          // slots made every date box in the app 16px wider than the date it holds, which is what
          // pushed the quick access of a period onto a second line. Focusing this field opens the
          // calendar anyway, so while the focus is here that button has nothing left to do — it is the
          // affordance of the *unfocused* box. Without the focus the way to clear is the reset button
          // in the calendar itself (see DateInputCalendar).
          <button
            type="button"
            // On pointer *down*, because the button unmounts as soon as the field is empty: by
            // pointerup it is gone, and a popover around it (the filter pill) would take that for a
            // click outside itself and close. Preventing the default also keeps the caret in the
            // field instead of blurring it.
            onPointerDown={(e) => {
              e.preventDefault();
              clear();
            }}
            aria-label={`${t("reset")}: ${ariaLabel ?? t("date._")}`}
            className="absolute inset-y-0.5 right-0.5 hidden w-5 cursor-pointer items-center justify-center text-muted-foreground group-focus-within:flex hover:text-foreground"
          >
            <HugeiconsIcon icon={Cancel01Icon} size={14} />
          </button>
        )}
        <DateInputCalendar
          // Out of the way while the clear button stands in its place — invisible rather than unmounted,
          // so the popover it anchors keeps its position and the focus does not fall to the document.
          hiddenWhileFocused={text !== "" && !disabled}
          value={value}
          defaultMonth={defaultMonth}
          onChange={onChange}
          disabled={disabled}
          open={pickerOpen}
          onOpenChange={setPickerOpen}
          fieldRef={inputRef}
          // Back into the field after a pick, so Tab carries on from here and a keyboard user is not
          // left on a button that just disappeared.
          onPicked={() => {
            refocusing.current = true;
            inputRef.current?.focus();
          }}
        />
      </div>
    </div>
  );
}
