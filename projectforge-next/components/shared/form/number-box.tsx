"use client";

import { useLayoutEffect, useRef, useState, type CSSProperties } from "react";
import { Input } from "@/components/ui/input";
import { useFormatContext } from "@/hooks/use-format";
import {
  formatNumberInput,
  groupNumberInput,
  numberLayout,
  parseGroupedInput,
  parseNumberInput,
  parsePercentInput,
} from "@/lib/number-parse";
import type { FormatContext } from "@/lib/format";
import { cn } from "@/lib/utils";

/** Rounds away what binary floating point adds — 0.19 * 100 is 19.000000000000004. */
function round(value: number, digits: number): number {
  return Number(value.toFixed(digits));
}

/**
 * The caret position in `grouped` that keeps it over the same digit it was in `typed`, ignoring the group
 * separators the two differ by. Counts the "stable" characters (digits, sign, the decimal separator) up to
 * the caret in the typed text, then walks the grouped text to just past that many of them — so an inserted
 * or removed thousands separator slides the caret along instead of stranding it.
 */
function caretAfterRegroup(
  typed: string,
  caret: number,
  grouped: string,
  ctx: FormatContext
): number {
  const { group, decimal } = numberLayout(ctx);
  const isStable = (ch: string) =>
    (ch >= "0" && ch <= "9") || ch === decimal || ch === "+" || ch === "-";
  let stableBefore = 0;
  for (let i = 0; i < caret && i < typed.length; i++) {
    if (typed[i] !== group && isStable(typed[i])) stableBefore++;
  }
  if (stableBefore === 0) return 0;
  let seen = 0;
  for (let i = 0; i < grouped.length; i++) {
    if (grouped[i] !== group && isStable(grouped[i])) {
      seen++;
      if (seen === stableBefore) return i + 1;
    }
  }
  return grouped.length;
}

export interface NumberBoxProps {
  id?: string;
  value: number | null;
  onChange: (next: number | null) => void;
  onBlur?: () => void;
  onFocus?: () => void;
  fractionDigits?: number;
  invalid?: boolean;
  disabled?: boolean;
  suffix?: string;
  maxDigits?: number;
  align?: "left" | "right";
  className?: string;
  "aria-label"?: string;
  /** Whether thousands are grouped while the box is at rest. */
  grouped?: boolean;
  /** Whether a share may be typed, and what of. */
  shareOf?: { amount: number | null | undefined };
}

/**
 * A decimal number typed in the user's layout and held as a `number` — the wire format of a `BigDecimal`.
 *
 * Like [DateInput] and unlike `<input type="number">`, the separators are the user's ([useFormatContext]),
 * not the browser's: "1.500,50" on a German account, "1500.50" on an English one, both saved as the same
 * number — and there are no spinner buttons. See lib/number-parse.ts for the reading side.
 *
 * The number and the text it is being typed as live side by side (the split [NumberSegmentInput] makes, for
 * the same reason): "1," is not yet a number, and rewriting it into "1" would correct the field under the
 * user's fingers. Used both by the form-bound [NumberField] and by hand-built controls that hold their own
 * state (the liquidity forecast's start amount).
 */
export function NumberBox({
  id,
  value,
  onChange,
  onBlur,
  onFocus,
  fractionDigits,
  invalid,
  disabled,
  suffix,
  maxDigits,
  align,
  className,
  "aria-label": ariaLabel,
  grouped,
  shareOf,
}: NumberBoxProps) {
  const ctx = useFormatContext();
  const inputRef = useRef<HTMLInputElement>(null);
  // The caret to restore after a keystroke regroups the text — set in `take`, applied once the DOM has the
  // regrouped value (below), then cleared. Grouping now stays visible while typing (see [groupNumberInput]).
  const caretRef = useRef<number | null>(null);
  const write = (n: number | null) =>
    formatNumberInput(n, ctx, fractionDigits, grouped);
  // The text a typed string becomes on screen: grouped as it is typed for a grouping box, verbatim
  // otherwise (a quantity or an order number is not grouped).
  const display = (typed: string) =>
    grouped ? groupNumberInput(typed, ctx) : typed;
  // `shows` is the number the text stands for: while it equals the value the text is ours and is left
  // alone. Adjusted during render, not in an effect, because it follows a value the *form* set —
  // loading an entity (form.reset) or the recalculated sums coming back.
  const [own, setOwn] = useState(() => ({
    text: write(value),
    shows: value,
  }));
  if (value !== own.shows) {
    setOwn({ text: write(value), shows: value });
  }
  const text = value === own.shows ? own.text : write(value);

  useLayoutEffect(() => {
    if (caretRef.current != null && inputRef.current) {
      inputRef.current.setSelectionRange(caretRef.current, caretRef.current);
      caretRef.current = null;
    }
  });

  /**
   * What a typed text does to the value — an amount, the share of a base a trailing "%" asks for, or
   * nothing yet. Not a pure "read the number" function on purpose: the three cases differ in what they
   * do to the text as well, and the box's rule is that a text it cannot read is left as typed.
   *
   * `caret` is where the caret sat in `typed`; when the text is regrouped it is carried across so the
   * inserted separators do not drag the caret to the end.
   */
  const take = (typed: string, caret: number) => {
    if (typed.trim() === "") {
      // An emptied box becomes null, which is how the backend stores "no value".
      setOwn({ text: typed, shows: null });
      onChange(null);
      return;
    }
    // A share ("50 %") is kept verbatim — a percentage is not a grouped amount.
    const share = shareOf ? parsePercentInput(typed, ctx) : null;
    if (share !== null) {
      const base = shareOf?.amount;
      if (base == null) {
        // A share of a base that isn't there yet is no more a number than a half-typed "1,": the text
        // stands (`shows: value` keeps the render above from rewriting it) and the value is untouched,
        // so a percentage never turns into the amount of bare digits nobody meant to enter.
        setOwn({ text: typed, shows: value });
        return;
      }
      // Rounded to the digits the box writes, so the amount that appears is the amount that is stored.
      // Wicket rounds the same way, at the scale of the total it computes against.
      const amount = round((base * share) / 100, fractionDigits ?? 2);
      setOwn({ text: typed, shows: amount });
      onChange(amount);
      return;
    }
    // Regroup first, then read the value off the regrouped text so the two never disagree (in a grouping
    // box the separator is grouping, not a decimal point — see [groupNumberInput]).
    const shown = display(typed);
    if (shown !== typed)
      caretRef.current = caretAfterRegroup(typed, caret, shown, ctx);
    // Not a number yet ("-", ",") keeps the text and the value it had, so nothing is lost while
    // typing. `shows: value` rather than `shows: null` — the render above rewrites the text whenever
    // the two disagree, which would put the deleted digit back under the caret: backspacing "-20" to
    // "-" would read as "still -20" and reappear as "-20".
    const parsed = grouped
      ? parseGroupedInput(shown, ctx)
      : parseNumberInput(shown, ctx);
    if (parsed === null) {
      setOwn({ text: shown, shows: value });
      return;
    }
    setOwn({ text: shown, shows: parsed });
    onChange(parsed);
  };

  return (
    <div
      className={cn("relative", maxDigits && "number-box-sized")}
      // The one kind of inline style this project allows: a CSS variable driving a class from
      // globals.css. A Tailwind arbitrary value cannot be built from a prop — the class would have to
      // exist in the source for the compiler to emit it.
      style={
        maxDigits
          ? ({
              "--number-box-digits": maxDigits,
              // The suffix sits inside the box, so it needs room of its own — the `pr-9` below.
              "--number-box-suffix": suffix ? "2.25rem" : "0rem",
            } as CSSProperties)
          : undefined
      }
    >
      <Input
        ref={inputRef}
        id={id}
        value={text}
        inputMode="decimal"
        autoComplete="off"
        disabled={disabled}
        aria-invalid={invalid || undefined}
        aria-label={ariaLabel}
        className={cn(
          "font-mono",
          align === "right" && "text-right",
          suffix && "pr-9",
          className
        )}
        onChange={(e) =>
          take(e.target.value, e.target.selectionStart ?? e.target.value.length)
        }
        onFocus={() => onFocus?.()}
        onBlur={() => {
          // Snap to the canonical form on blur — pad to `fractionDigits` and drop a trailing separator.
          setOwn({ text: write(value), shows: value });
          onBlur?.();
        }}
      />
      {suffix && (
        <span
          aria-hidden
          className="pointer-events-none absolute inset-y-0 right-2.5 flex items-center text-xs text-muted-foreground"
        >
          {suffix}
        </span>
      )}
    </div>
  );
}
