"use client";

import { useRef, useState } from "react";
import { Input } from "@/components/ui/input";
import { useFormatContext } from "@/hooks/use-format";
import {
  formatTimeInput,
  parseTimeInput,
  timeOf,
  timePartsOf,
  timePatternOf,
} from "@/lib/time-parse";
import { cn } from "@/lib/utils";
import { TimeInputPicker } from "./time-input-picker";

export interface TimeInputProps {
  /** The time as `HH:mm` on a 24h clock, whatever notation is shown (see lib/time-parse.ts). */
  value: string | null | undefined;
  onChange: (value: string | null) => void;
  id?: string;
  /** Accessible name, when no `<label htmlFor>` names the field. */
  "aria-label"?: string;
  className?: string;
  disabled?: boolean;
  /** Enter; passes the value in effect afterwards, as [DateInput] does. */
  onSubmit?: (value: string | null) => void;
}

/**
 * A time of day, typed in the notation the account has chosen.
 *
 * Text rather than `<input type="time">` for the same reason [DateInput] is not `type="date"`: the
 * native field takes its 12h/24h presentation from the operating system, so an H24 account on an
 * English machine would be shown "2:30 PM" while the table next to it reads "14:33". Notation comes
 * from `userData.timeNotation` through [useFormatContext].
 *
 * Committing follows [DateInput]: strict while typing, lenient on blur and Enter, so "930" and
 * "2:30p" both work but nothing is rewritten mid-word. Beside the field sits [TimeInputPicker], the
 * dropdown of hours and minutes the legacy webapp had — what the native field would have given for
 * free, and the reason typing is not the only way in.
 */
export function TimeInput({
  value,
  onChange,
  id,
  className,
  disabled,
  onSubmit,
  "aria-label": ariaLabel,
}: TimeInputProps) {
  const ctx = useFormatContext();
  const [text, setText] = useState(() => formatTimeInput(value, ctx));
  const inputRef = useRef<HTMLInputElement>(null);
  const parts = timePartsOf(value);
  // Opened by focusing the field, as [DateInput] opens its calendar: the picker is the usual way in,
  // and reaching for the button beside it first would be a step nobody expects.
  const [pickerOpen, setPickerOpen] = useState(false);

  // The value can change without this field being touched (a preset click, a filter being restored).
  // Adjusted during render, as React prescribes for state derived from props, and only when it means
  // a different time — otherwise a half-typed "9:" would be rewritten under the user's fingers.
  const [synced, setSynced] = useState({ value, hour12: ctx.hour12 });
  if (synced.value !== value || synced.hour12 !== ctx.hour12) {
    setSynced({ value, hour12: ctx.hour12 });
    if (parseTimeInput(text, ctx) !== (value ?? null)) {
      setText(formatTimeInput(value, ctx));
    }
  }

  /** Reads the text and returns the value in effect afterwards. */
  function commit(raw: string): string | null {
    if (raw.trim() === "") {
      onChange(null);
      setText("");
      return null;
    }
    const parsed = parseTimeInput(raw, ctx);
    // Not a time: keep the value and put its text back, rather than dropping what is there.
    setText(formatTimeInput(parsed ?? value, ctx));
    if (parsed) onChange(parsed);
    return parsed ?? value ?? null;
  }

  return (
    <div className={cn("flex items-center gap-1", className)}>
      <Input
        ref={inputRef}
        id={id}
        aria-label={ariaLabel}
        disabled={disabled}
        inputMode="numeric"
        type="text"
        placeholder={timePatternOf(ctx)}
        value={text}
        className="flex-1 text-xs"
        onFocus={() => setPickerOpen(true)}
        onChange={(e) => {
          setText(e.target.value);
          // Only a fully written time is committed while typing; "9" would otherwise become 09:00
          // before the minutes are there.
          const full = /^\s*\d{1,2}\s*[:.]\s*\d{2}/.test(e.target.value);
          const parsed = full ? parseTimeInput(e.target.value, ctx) : null;
          if (parsed) onChange(parsed);
          else if (e.target.value.trim() === "") onChange(null);
        }}
        onBlur={() => commit(text)}
        onKeyDown={(e) => {
          if (e.key !== "Enter") return;
          e.preventDefault();
          setPickerOpen(false);
          onSubmit?.(commit(text));
        }}
      />
      <TimeInputPicker
        hour={parts?.[0] ?? null}
        minute={parts?.[1] ?? null}
        disabled={disabled}
        onChange={(hour, minute) => onChange(timeOf(hour, minute))}
        open={pickerOpen}
        onOpenChange={setPickerOpen}
        fieldRef={inputRef}
      />
    </div>
  );
}
