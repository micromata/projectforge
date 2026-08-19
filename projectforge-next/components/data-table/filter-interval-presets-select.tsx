"use client";

import { useTranslations } from "next-intl";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useFormatContext } from "@/hooks/use-format";
import { nowIso } from "@/lib/user-zone";
import type { MagicFilterEntryValue } from "@/lib/rs/types";
import { INTERVAL_PRESETS } from "./history-interval-presets";

/**
 * The rolling windows a TIMESTAMP filter offers when the backend marks it `UNTIL_NOW`: "die letzten
 * 30 Minuten", "seit gestern". Every one of them ends at now, which is what distinguishes them from
 * the calendar periods of [PeriodStepper] — those begin and end on a month boundary.
 *
 * A list, not a row of chips: thirteen periods as buttons is a wall of text above the two bounds they
 * set, and only one of them can be in effect at a time anyway.
 */
export function IntervalPresetsSelect({
  onChange,
  className,
}: {
  onChange: (value: MagicFilterEntryValue) => void;
  className?: string;
}) {
  const t = useTranslations("filter");
  const tSearch = useTranslations("search");
  const ctx = useFormatContext();

  return (
    <Select
      // No value of its own: a period is a shortcut for filling the two bounds, and once one of them
      // is edited by hand no period is "selected" any more.
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
        className={className}
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
  );
}
