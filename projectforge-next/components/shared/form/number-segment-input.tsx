"use client";

import { useState, type KeyboardEvent } from "react";
import { Input } from "@/components/ui/input";
import {
  displaySegment,
  segmentDigits,
  segmentValue,
  stepSegment,
  type NumberSegment,
} from "@/lib/form/number-segments";
import { cn } from "@/lib/utils";
import { useEntityEditForm } from "./form-context";

export interface NumberSegmentInputProps {
  segment: NumberSegment;
  /** Accessible name of the box, e.g. "Kostenträger: Bereich" — the group's label plus its own. */
  ariaLabel: string;
  invalid: boolean;
  /** The box is full — the group moves the focus on. */
  onFilled: () => void;
  /** Backspace or an arrow at an edge — the group focuses the box before or after this one. */
  onLeave: (direction: "prev" | "next") => void;
  registerInput: (el: HTMLInputElement | null) => void;
  onPasteSegments: (text: string) => void;
}

/** One box of a [SegmentedNumberField], bound to its own form value. */
export function NumberSegmentInput(props: NumberSegmentInputProps) {
  const form = useEntityEditForm();
  return (
    <form.Field name={props.segment.name as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => (
        <SegmentBox
          {...props}
          value={field.state.value as number | null}
          onChange={(next: number | null) => field.handleChange(next)}
          onBlurField={field.handleBlur}
        />
      )}
    </form.Field>
  );
}

function SegmentBox({
  segment,
  ariaLabel,
  invalid,
  value,
  onChange,
  onBlurField,
  onFilled,
  onLeave,
  registerInput,
  onPasteSegments,
}: NumberSegmentInputProps & {
  value: number | null;
  onChange: (next: number | null) => void;
  onBlurField: () => void;
}) {
  // The box keeps its own text so a half-typed value can be shown and the padding applied on blur
  // without ever changing the number the form holds. `shows` is the number that text stands for: while
  // it equals the form's value the text is ours and must be left alone — typing "1" into a three-digit
  // box holds "1", although the same number renders as "001" when it comes back from the form.
  const [own, setOwn] = useState(() => ({
    text: displaySegment(value, segment.digits),
    shows: value,
  }));

  // Adjusted during render, not in an effect: this follows a value the *form* set — loading an entity
  // (form.reset) or a number pasted into a neighbouring box.
  if (value !== own.shows) {
    setOwn({ text: displaySegment(value, segment.digits), shows: value });
  }
  const text =
    value === own.shows ? own.text : displaySegment(value, segment.digits);

  function show(next: number | null) {
    setOwn({ text: displaySegment(next, segment.digits), shows: next });
    onChange(next);
  }

  function commit(typed: string) {
    const digits = segmentDigits(typed, segment.digits);
    const number = segmentValue(digits);
    setOwn({ text: digits, shows: number });
    onChange(number);
    if (digits.length === segment.digits) onFilled();
  }

  function onKeyDown(e: KeyboardEvent<HTMLInputElement>) {
    const caret = e.currentTarget.selectionStart ?? 0;
    if (
      (e.key === "Backspace" && text === "") ||
      (e.key === "ArrowLeft" && caret === 0)
    ) {
      e.preventDefault();
      onLeave("prev");
    } else if (e.key === "ArrowRight" && caret === text.length) {
      e.preventDefault();
      onLeave("next");
    } else if (e.key === "ArrowUp" || e.key === "ArrowDown") {
      e.preventDefault();
      show(stepSegment(value, segment, e.key === "ArrowUp" ? 1 : -1));
    } else if (e.key.length === 1 && /\D/.test(e.key) && !e.metaKey) {
      // A separator typed by hand ("." in a cost number) moves on instead of being swallowed.
      e.preventDefault();
      onLeave("next");
    }
  }

  return (
    <Input
      ref={registerInput}
      value={text}
      inputMode="numeric"
      autoComplete="off"
      maxLength={segment.digits}
      aria-label={ariaLabel}
      aria-invalid={invalid || undefined}
      onChange={(e) => commit(e.target.value)}
      onKeyDown={onKeyDown}
      onPaste={(e) => {
        const pasted = e.clipboardData.getData("text");
        // A whole number pasted into any box fills the group; a single segment falls through to
        // onChange, which truncates it like typing would.
        if (/\D/.test(pasted)) {
          e.preventDefault();
          onPasteSegments(pasted);
        }
      }}
      onBlur={() => {
        // Padding becomes visible only now, so it can't fight what is being typed.
        setOwn({ text: displaySegment(value, segment.digits), shows: value });
        onBlurField();
      }}
      className={cn(
        "text-center font-mono",
        segment.digits > 2 ? "w-16" : "w-12"
      )}
    />
  );
}
