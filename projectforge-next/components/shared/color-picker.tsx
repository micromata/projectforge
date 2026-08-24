"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { CheckmarkCircle02Icon } from "@hugeicons/core-free-icons";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

/** The backend's own rule (`CalendarStyle.validateHexCode`); `changeStyle` throws for anything else. */
const HEX_RE = /^#([0-9a-f]{3}|[0-9a-f]{6})$/i;

export function isValidHexColor(value: string): boolean {
  return HEX_RE.test(value.trim());
}

/**
 * A palette of ready-made calendar colours. These are colour **data** stored per calendar
 * (`CalendarStyle.bgColor`), not theme tokens — the same kind of value FullCalendar paints an event
 * with — so each swatch is rendered from its hex directly, as the event dots already are. Seeded from
 * the brand palette plus a few saturated tones so a calendar can be told apart at a glance.
 */
const PRESET_COLORS = [
  "#009ba3",
  "#007480",
  "#e10057",
  "#a70836",
  "#ffcb00",
  "#ff9d01",
  "#75be01",
  "#32912e",
  "#3f51b5",
  "#9c27b0",
  "#795548",
  "#607d8b",
] as const;

interface ColorPickerProps {
  /** The current colour, a hex string (`#rgb` or `#rrggbb`); empty means none chosen yet. */
  value?: string | null;
  /** Fires with a valid hex whenever the user settles on a colour (swatch, native picker, valid input). */
  onChange: (value: string) => void;
  id?: string;
  "aria-label"?: string;
  /** Marks the hex input as erroneous, for a server-side validation error on the field. */
  invalid?: boolean;
  className?: string;
}

/**
 * Picks a colour three ways — a preset swatch, the OS-native colour dialog, or a typed hex code — and
 * reports it back as a validated hex string. It never talks to the backend itself: the caller decides
 * what a chosen colour means (write it into a form, or fire `changeStyle` once the popover closes), so
 * this stays reusable between the calendar's style popover and the `COLOR_CHOOSER` form field.
 */
export function ColorPicker({
  value,
  onChange,
  id,
  className,
  invalid,
  "aria-label": ariaLabel,
}: ColorPickerProps) {
  const t = useTranslations();
  // The text field keeps its own draft so a half-typed "#0" is not pushed out as a colour; only a
  // complete, valid hex reaches `onChange`.
  const [draft, setDraft] = useState(value ?? "");
  const current = value ?? "";

  const commit = (next: string) => {
    setDraft(next);
    if (isValidHexColor(next)) onChange(next.trim().toLowerCase());
  };

  return (
    <div className={cn("flex flex-col gap-2", className)}>
      <div className="flex flex-wrap gap-1.5">
        {PRESET_COLORS.map((color) => {
          const selected = current.toLowerCase() === color;
          return (
            <button
              key={color}
              type="button"
              onClick={() => commit(color)}
              aria-label={color}
              aria-pressed={selected}
              className="flex size-6 items-center justify-center rounded-full border border-border/60"
              style={{ background: color }}
            >
              {selected && (
                <HugeiconsIcon
                  icon={CheckmarkCircle02Icon}
                  size={14}
                  className="text-white drop-shadow"
                  aria-hidden
                />
              )}
            </button>
          );
        })}
      </div>
      <div className="flex items-center gap-2">
        {/* A labelled swatch over the sr-only native input: OS-native, keyboard-reachable, zero deps. */}
        <label
          className="size-8 shrink-0 cursor-pointer rounded-md border border-input"
          style={{
            background: isValidHexColor(current) ? current : "transparent",
          }}
        >
          <span className="sr-only">{t("select")}</span>
          <input
            type="color"
            value={isValidHexColor(current) ? current : "#000000"}
            onChange={(e) => commit(e.target.value)}
            className="sr-only"
            aria-label={ariaLabel ?? t("select")}
          />
        </label>
        <Input
          id={id}
          value={draft}
          onChange={(e) => commit(e.target.value)}
          placeholder="#rrggbb"
          aria-invalid={
            invalid || (draft.length > 0 && !isValidHexColor(draft))
          }
          aria-label={ariaLabel}
          className="h-8 w-28 font-mono text-xs"
        />
      </div>
    </div>
  );
}
