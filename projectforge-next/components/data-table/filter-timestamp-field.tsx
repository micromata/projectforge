"use client";

import { useTranslations } from "next-intl";
import { DateTimeInput } from "@/components/shared/date-time-input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useFormatContext } from "@/hooks/use-format";
import { DEFAULT_TO_TIME, nowIso } from "@/lib/user-zone";
import type { FilterElement, MagicFilterEntryValue } from "@/lib/rs/types";
import type { FilterInputProps } from "./filter-field-inputs";
import { INTERVAL_PRESETS } from "./history-interval-presets";

/**
 * A TIMESTAMP filter (org.projectforge.ui.filter.UIFilterTimestampElement): a range of instants,
 * either bound optional (`openInterval`).
 *
 * Both bounds carry a time of day, which a DATE filter's two date inputs cannot express — and must:
 * `PFDateTimeUtils.parseAndCreateDateTime` parses a timestamp field with
 * `parseLocalDateIfNoTimeOfDayGiven = false`, so a bare `2026-07-15` yields null and the bound is
 * silently dropped.
 *
 * The quick-select periods appear when the backend marks the field `UNTIL_NOW`, the selector that
 * means "last x minutes, hours, days" — for the history filter it does, and like Wicket they sit in
 * a dropdown rather than in a row of buttons.
 */
export function TimestampRangeField({
  element,
  value,
  onChange,
  label,
  autoFocus,
  onSubmit,
}: FilterInputProps & { element: FilterElement }) {
  const t = useTranslations("filter");
  const tSearch = useTranslations("search");
  const ctx = useFormatContext();
  const showPresets = element.selectors?.includes("UNTIL_NOW") ?? false;

  function next(
    part: "from" | "to",
    iso: string | null
  ): MagicFilterEntryValue | undefined {
    const merged = { ...value, [part]: iso ?? undefined };
    return merged.from || merged.to ? merged : undefined;
  }

  return (
    <div className="space-y-1.5">
      <p className="text-xs font-medium">{label}</p>
      <div className="space-y-1">
        <DateTimeInput
          autoFocus={autoFocus}
          dateLabel={`${label}: ${t("dateFrom")}`}
          timeLabel={`${label}: ${t("timeFrom")}`}
          value={value?.from}
          onChange={(iso) => onChange(next("from", iso))}
          // The instant the input just committed — `value` here is still the previous one.
          onSubmit={(iso) => onSubmit?.(next("from", iso))}
        />
        <DateTimeInput
          dateLabel={`${label}: ${t("dateTo")}`}
          timeLabel={`${label}: ${t("timeTo")}`}
          // A date typed as the end of a range means the whole day, not its midnight.
          fallbackTime={DEFAULT_TO_TIME}
          value={value?.to}
          onChange={(iso) => onChange(next("to", iso))}
          onSubmit={(iso) => onSubmit?.(next("to", iso))}
        />
      </div>
      {showPresets && (
        // A list, not a row of chips: thirteen periods as buttons is a wall of text above the two
        // bounds they set, and only one of them can be in effect at a time anyway.
        <Select
          // No value of its own: a period is a shortcut for filling the two bounds, and once one of
          // them is edited by hand no period is "selected" any more.
          value=""
          onValueChange={(id) => {
            const preset = INTERVAL_PRESETS.find((entry) => entry.id === id);
            if (!preset) return;
            // "Now" is read once, so both bounds belong to the same instant.
            const now = nowIso();
            const from = preset.from(now, ctx);
            if (from) onChange({ from, to: now });
          }}
        >
          <SelectTrigger
            // Named explicitly: with no value selected the trigger shows only the placeholder, which
            // Radix marks as such rather than as the control's label.
            aria-label={t("periodChoose")}
            size="sm"
            className="mt-0.5 w-full text-xs"
          >
            <SelectValue placeholder={t("periodChoose")} />
          </SelectTrigger>
          <SelectContent>
            {INTERVAL_PRESETS.map((preset) => (
              <SelectItem key={preset.id} value={preset.id} className="text-xs">
                {tSearch(
                  preset.key,
                  preset.arg == null ? {} : { arg0: preset.arg }
                )}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      )}
    </div>
  );
}
