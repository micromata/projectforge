"use client";

import { useState, type CSSProperties } from "react";
import { Input } from "@/components/ui/input";
import { useFormatContext } from "@/hooks/use-format";
import { formatNumberInput, parseNumberInput } from "@/lib/number-parse";
import { cn } from "@/lib/utils";
import {
  FieldShell,
  useFieldIds,
  type BaseFieldProps,
  type FieldMetaState,
} from "./field-shell";
import { useEntityEditForm, useFieldMetadata } from "./form-context";
import { useFieldErrors } from "./use-field-errors";

export interface NumberFieldProps extends BaseFieldProps {
  /**
   * Digits after the decimal separator. Defaults to 2 for an `AMOUNT` and to none for anything else,
   * i.e. to what the value is: an amount is written 1.500,00 even when it is round, a quantity like
   * person days is not.
   */
  fractionDigits?: number;
  /** The currency behind the box, for an amount. Comes from the user's settings, never spelled out. */
  suffix?: string;
  disabled?: boolean;
  /**
   * Digits of the widest value this field can hold — the box stops growing there instead of taking the
   * whole column: an order's number is six digits, a percentage three.
   *
   * Not derivable from the metadata: a column's precision is not a digit count, and the range that
   * bounds a percentage lives in the schema (`probabilityOfOccurrence`), so the declaration says it.
   */
  maxDigits?: number;
  /**
   * Where the digits sit in the box. Left by default — a form is read down its labels, and a number
   * pushed to the far end of its box reads as detached from the one naming it.
   *
   * The opposite of a *list*, where a column of numbers is compared line by line and therefore aligns
   * on the right (see the `align` of a column declaration).
   */
  align?: "left" | "right";
  /**
   * The field holds a **factor** and the box shows a **percentage**: 0.19 is typed and read as 19.
   *
   * An invoice position's VAT rate is stored that way (`RechnungsPositionDO.vat`) while nobody enters a
   * VAT rate as 0.19 — Wicket wraps the same field in a `BigDecimalPercentConverter` for exactly this
   * reason. Here rather than in the calling feature, for the reason [SelectField]'s `valueType` is here:
   * the point of these components is that a field binds to what the entity declares, so a conversion
   * done at the call site would have to be repeated by every caller and undone on every read.
   *
   * Not to be confused with a field that already holds a percentage (`discountPercent`, an order's
   * `probabilityOfOccurrence`) — that needs nothing but a `%` suffix.
   */
  percent?: boolean;
}

/** Digits the factor behind a percentage is rounded to: 19 % is 0.19, 19,25 % is 0.1925. */
const FACTOR_DIGITS = 6;

/** Rounds away what binary floating point adds — 0.19 * 100 is 19.000000000000004. */
function round(value: number, digits: number): number {
  return Number(value.toFixed(digits));
}

/** The percentage a stored factor stands for: 0.19 → 19. See [NumberFieldProps.percent]. */
function toPercentage(value: number | null): number | null {
  return value == null ? null : round(value * 100, FACTOR_DIGITS - 2);
}

/** The factor a typed percentage stands for: 19 → 0.19. */
function toFactor(value: number | null): number | null {
  return value == null ? null : round(value / 100, FACTOR_DIGITS);
}

/**
 * A decimal number, typed in the user's layout and held as a `number` — the wire format of a
 * `BigDecimal`.
 *
 * Like [DateInput] and unlike `<input type="number">`, the separators are the user's
 * ([useFormatContext]), not the browser's: "1.500,50" on a German account, "1500.50" on an English
 * one, both saved as the same number. See lib/number-parse.ts for the reading side.
 */
export function NumberField({
  name,
  label,
  hint,
  className,
  fractionDigits,
  suffix,
  disabled,
  maxDigits,
  align,
  percent,
}: NumberFieldProps) {
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  const { required, dataType } = useFieldMetadata(name);
  const digits = fractionDigits ?? (dataType === "AMOUNT" ? 2 : undefined);
  return (
    <form.Field name={name as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as FieldMetaState;
        const invalid = meta.isTouched && !meta.isValid;
        return (
          <FieldShell
            label={label}
            required={required}
            readOnly={disabled}
            hint={hint}
            invalid={invalid}
            errors={fieldErrors(meta, label)}
            className={className}
            ids={ids}
          >
            <NumberBox
              id={ids.controlId}
              value={
                percent
                  ? toPercentage(field.state.value as number | null)
                  : (field.state.value as number | null)
              }
              onChange={(next) =>
                field.handleChange(percent ? toFactor(next) : next)
              }
              onBlur={field.handleBlur}
              fractionDigits={digits}
              invalid={invalid}
              disabled={disabled}
              suffix={suffix}
              maxDigits={maxDigits}
              align={align}
            />
          </FieldShell>
        );
      }}
    </form.Field>
  );
}

/**
 * The box itself, separated from the form binding so the number and the text it is being typed as can
 * live side by side — the same split [NumberSegmentInput] makes, and for the same reason: "1," is not
 * yet a number, and rewriting it into "1" would correct the field under the user's fingers.
 */
function NumberBox({
  id,
  value,
  onChange,
  onBlur,
  fractionDigits,
  invalid,
  disabled,
  suffix,
  maxDigits,
  align,
}: {
  id: string;
  value: number | null;
  onChange: (next: number | null) => void;
  onBlur: () => void;
  fractionDigits?: number;
  invalid: boolean;
  disabled?: boolean;
  suffix?: string;
  maxDigits?: number;
  align?: "left" | "right";
}) {
  const ctx = useFormatContext();
  // `shows` is the number the text stands for: while it equals the value the text is ours and is left
  // alone. Adjusted during render, not in an effect, because it follows a value the *form* set —
  // loading an entity (form.reset) or the recalculated sums coming back.
  const [own, setOwn] = useState(() => ({
    text: formatNumberInput(value, ctx, fractionDigits),
    shows: value,
  }));
  if (value !== own.shows) {
    setOwn({
      text: formatNumberInput(value, ctx, fractionDigits),
      shows: value,
    });
  }
  const text =
    value === own.shows
      ? own.text
      : formatNumberInput(value, ctx, fractionDigits);

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
        id={id}
        value={text}
        inputMode="decimal"
        autoComplete="off"
        disabled={disabled}
        aria-invalid={invalid || undefined}
        className={cn(
          "font-mono",
          align === "right" && "text-right",
          suffix && "pr-9"
        )}
        onChange={(e) => {
          const typed = e.target.value;
          const parsed = parseNumberInput(typed, ctx);
          // Not a number yet ("1,") keeps the text and the value it had, so nothing is lost while
          // typing; an emptied box becomes null, which is how the backend stores "no value".
          setOwn({ text: typed, shows: typed.trim() === "" ? null : parsed });
          if (typed.trim() === "") onChange(null);
          else if (parsed !== null) onChange(parsed);
        }}
        onBlur={() => {
          // The padding to `fractionDigits` becomes visible only now, so it can't fight what is
          // being typed.
          setOwn({
            text: formatNumberInput(value, ctx, fractionDigits),
            shows: value,
          });
          onBlur();
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
