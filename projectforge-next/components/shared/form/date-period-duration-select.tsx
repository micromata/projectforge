"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import { Timer01Icon } from "@hugeicons/core-free-icons";
import { useTranslations } from "next-intl";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type { Duration } from "@/lib/date-duration";
import { cn } from "@/lib/utils";

/**
 * The lengths a period can be given: "1 Monat" fills the end in from the begin (see
 * [useDatePeriodDuration]).
 *
 * As narrow as it can be made, because it has to fit behind two date boxes in a single column of a form
 * grid: the trigger says "3M" and, with nothing in effect, shows only its icon, while the list that opens
 * spells every length out ("3 Monate"). The two texts are the same statement in two widths — the short one
 * never has to be understood on its own, since it is only ever read right after being picked from the long
 * one, and the trigger carries the full name as its `title` besides.
 *
 * A list rather than a row of buttons, as the filter's [IntervalPresetsSelect] is: four terms as chips
 * would be as wide as the two dates they belong to, and only one of them can be in effect anyway.
 *
 * No "no duration" entry. With nothing in effect the trigger already shows its placeholder, and an entry
 * for it would have to *do* something when picked — clear the end? leave it? — where neither is a thing
 * anybody means. A term is dissolved by editing the end, which is the box the user reaches for.
 */
export function DatePeriodDurationSelect({
  durations,
  value,
  onSelect,
  disabled,
  className,
}: {
  durations: readonly Duration[];
  /** The duration in effect, or null while the two ends are none. */
  value: Duration | null;
  onSelect: (duration: Duration) => void;
  disabled?: boolean;
  className?: string;
}) {
  const t = useTranslations();
  if (!durations.length) return null;

  const name = (duration: Duration, short = false) =>
    t(
      short ? duration.shortLabelKey : duration.labelKey,
      duration.labelArg == null ? {} : { arg0: duration.labelArg }
    );

  return (
    <Select
      // Empty rather than absent, so Radix falls back to the placeholder and "no term in effect" is
      // visible in the trigger itself.
      value={value?.id ?? ""}
      disabled={disabled}
      onValueChange={(id) => {
        const picked = durations.find((duration) => duration.id === id);
        if (picked) onSelect(picked);
      }}
    >
      <SelectTrigger
        // Named explicitly: the trigger shows an abbreviation or nothing but an icon, neither of which
        // says what the control is — and with nothing selected Radix marks its text as a placeholder
        // rather than as the label.
        aria-label={t("duration.choose")}
        title={value ? name(value) : t("duration.choose")}
        size="sm"
        // Tighter than a select of words: two characters and a caret, so the whole quick access still
        // fits on the line the two date boxes leave.
        className={cn("w-auto gap-0.5 px-1", className)}
      >
        {/* Children rather than the item's own text, which would be the spelled-out name: Radix falls
            back to them only while something is selected, so the placeholder below still shows. */}
        <SelectValue
          placeholder={
            <HugeiconsIcon
              icon={Timer01Icon}
              className="text-muted-foreground"
            />
          }
        >
          {value && name(value, true)}
        </SelectValue>
      </SelectTrigger>
      <SelectContent>
        {durations.map((duration) => (
          <SelectItem key={duration.id} value={duration.id}>
            {name(duration)}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
