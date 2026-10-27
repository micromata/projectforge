"use client";

import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { NumberBox } from "@/components/shared/form/number-box";
import { cn } from "@/lib/utils";

/** Quick picks next to the free input — days from the base date on. */
export const NEXT_DAYS_PRESETS = [30, 60, 90, 180, 365] as const;
/** The backend clamps the horizon to 1..600 (`MAX_FORECAST_DAYS`). */
export const MAX_NEXT_DAYS = 600;

/** Clamps a typed horizon into what the backend accepts; an empty box counts as [fallback]. */
export function clampNextDays(days: number | null, fallback: number): number {
  if (days == null) return fallback;
  return Math.min(MAX_NEXT_DAYS, Math.max(1, Math.round(days)));
}

/**
 * The forecast horizon: any number of days (1..600) typed freely, plus quick-pick buttons for the usual
 * ones. The box may be empty while typing; the caller reads it through [clampNextDays].
 */
export function LiquidityForecastDaysInput({
  label,
  value,
  onChange,
}: {
  label: string;
  value: number | null;
  onChange: (days: number | null) => void;
}) {
  const clamp = (days: number | null) =>
    days == null ? null : Math.min(MAX_NEXT_DAYS, days);
  return (
    <div className="grid gap-1.5">
      <Label htmlFor="nextDays">{label}</Label>
      <div className="flex items-center gap-1.5">
        <NumberBox
          id="nextDays"
          value={value}
          onChange={(days) => onChange(clamp(days))}
          onBlur={() => onChange(clampNextDays(value, 1))}
          fractionDigits={0}
          align="right"
          className="w-20"
        />
        {NEXT_DAYS_PRESETS.map((days) => (
          <Button
            key={days}
            type="button"
            size="sm"
            variant={days === value ? "default" : "outline"}
            aria-label={`${label} ${days}`}
            aria-pressed={days === value}
            className={cn("px-2")}
            onClick={() => onChange(days)}
          >
            {days}
          </Button>
        ))}
      </div>
    </div>
  );
}
