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

  /** Reads the text and returns the value that is in effect afterwards. */
  function commit(raw: string): string | null {
    if (raw.trim() === "") {
      onChange(null);
      setText("");
      return null;
    }
    const parsed = parseDateInput(raw, ctx);
    // Not a date: keep the value and put its text back, rather than silently dropping what is there.
    setText(formatDateInput(parsed ?? value, ctx));
    if (parsed) onChange(parsed);
    return parsed ?? value ?? null;
  }

  function clear() {
    setText("");
    onChange(null);
    inputRef.current?.focus();
  }

  return (
    <div className={cn("flex items-center gap-1", className)}>
      <div className="relative flex-1">
        <Input
          ref={inputRef}
          id={id}
          aria-label={ariaLabel}
          aria-invalid={invalid || undefined}
          autoFocus={autoFocus}
          disabled={disabled}
          required={required}
          inputMode="numeric"
          // Text, not "date": the mask is the user's, not the browser's.
          type="text"
          placeholder={ctx.datePattern}
          value={text}
          className={cn(text !== "" && "pr-7")}
          onFocus={() => {
            if (!refocusing.current) setPickerOpen(true);
            refocusing.current = false;
          }}
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
          // it is a chore. Also reachable from the calendar popover and via Escape.
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
            className="absolute inset-y-0 right-1.5 flex cursor-pointer items-center text-muted-foreground hover:text-foreground"
          >
            <HugeiconsIcon icon={Cancel01Icon} size={12} />
          </button>
        )}
      </div>
      <DateInputCalendar
        value={value}
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
  );
}
