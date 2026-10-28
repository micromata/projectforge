"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  ArrowDown01Icon,
  CheckmarkCircle02Icon,
} from "@hugeicons/core-free-icons";
import { HexColorPicker } from "react-colorful";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import { PRESET_COLORS } from "./color-palette";

/** The backend's own rule (`CalendarStyle.validateHexCode`); `changeStyle` throws for anything else. */
const HEX_RE = /^#([0-9a-f]{3}|[0-9a-f]{6})$/i;

export function isValidHexColor(value: string): boolean {
  return HEX_RE.test(value.trim());
}

interface ColorPickerProps {
  /** The current colour, a hex string (`#rgb` or `#rrggbb`); empty means none chosen yet. */
  value?: string | null;
  /** Fires with a valid hex whenever the user settles on a colour (swatch, picker, valid input). */
  onChange: (value: string) => void;
  id?: string;
  "aria-label"?: string;
  /** Marks the hex input as erroneous, for a server-side validation error on the field. */
  invalid?: boolean;
  className?: string;
}

/**
 * Picks a colour three ways — a preset swatch, any colour in the picker area (react-colorful, folded
 * behind "Custom colour" since a swatch is the usual choice), or a typed hex code — and reports it back
 * as a validated, lower-cased hex string. It never talks to the backend itself: the caller decides what a
 * chosen colour means (write it into a form, or fire `changeStyle` once the popover closes), so this
 * stays reusable between the calendar's style popover and the colour fields of the forms.
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
  const current = (value ?? "").toLowerCase();
  const isPreset = PRESET_COLORS.includes(current);
  // A colour of no swatch was picked freely: the picker shows it.
  const [custom, setCustom] = useState(!!current && !isPreset);

  const commit = (next: string) => {
    setDraft(next);
    if (isValidHexColor(next)) onChange(next.trim().toLowerCase());
  };

  return (
    <div className={cn("flex w-fit flex-col gap-2", className)}>
      <div className="grid grid-cols-[repeat(11,1.25rem)] gap-1">
        {PRESET_COLORS.map((color) => {
          const selected = current === color;
          return (
            <button
              key={color}
              type="button"
              onClick={() => commit(color)}
              aria-label={color}
              aria-pressed={selected}
              className="flex size-5 items-center justify-center rounded-full border border-border/60"
              style={{ background: color }}
            >
              {selected && (
                <HugeiconsIcon
                  icon={CheckmarkCircle02Icon}
                  size={12}
                  className="text-white drop-shadow"
                  aria-hidden
                />
              )}
            </button>
          );
        })}
      </div>
      <button
        type="button"
        onClick={() => setCustom((open) => !open)}
        aria-expanded={custom}
        className="flex items-center gap-1 self-start text-xs text-muted-foreground hover:text-foreground"
      >
        <HugeiconsIcon
          icon={ArrowDown01Icon}
          size={14}
          className={cn("transition-transform", !custom && "-rotate-90")}
          aria-hidden
        />
        {t("color.picker.custom")}
      </button>
      {custom && (
        <HexColorPicker
          color={isValidHexColor(current) ? current : PRESET_COLORS[0]}
          onChange={commit}
          style={{ width: "100%", height: "9rem" }}
        />
      )}
      <div className="flex items-center gap-2">
        <span
          aria-hidden
          className="size-8 shrink-0 rounded-md border border-input"
          style={{
            background: isValidHexColor(current) ? current : "transparent",
          }}
        />
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
