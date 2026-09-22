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
import { CUSTOM_PERIOD_KIND, type PeriodKind } from "@/lib/date-period";
import { leafKeyOf } from "@/lib/leaf-key";
import { cn } from "@/lib/utils";

/**
 * The art a period is given as: "3 Monate" fills the end in from the begin, "Jahr bis heute" runs it up
 * to today (see lib/date-period.ts).
 *
 * As narrow as it can be made, because it has to fit behind two date boxes in a single column of a form
 * grid: the trigger says "3M" and, with nothing in effect, shows only its icon, while the list that opens
 * spells every art out ("3 Monate"). The two texts are the same statement in two widths — the short one
 * never has to be understood on its own, since it is only ever read right after being picked from the long
 * one, and the trigger carries the full name as its `title` besides.
 *
 * A list rather than a row of buttons, as the filter's [IntervalPresetsSelect] is: four terms as chips
 * would be as wide as the two dates they belong to, and only one of them can be in effect anyway.
 *
 * A first "Eigener Zeitraum" entry appears wherever a caller passes [onClear] and an art is in effect. It
 * releases the art while keeping the two dates, so moving the begin no longer drags the end along — the
 * one thing editing a box cannot do: in a filter, retyping the begin under a `month` re-snaps it to the
 * first; on a form, it drags the end to hold the term. What "letting go" means differs by surface (a
 * filter remembers the art, a form derives it from the dates), which is why the caller passes the handler.
 *
 * Picking the art that is already in effect is not a no-op: it sets the *current* period of that art, which
 * is how one gets back to this month after paging away (the entry says so in its tooltip).
 */
export function PeriodQuickSelect({
  kinds,
  value,
  onSelect,
  onClear,
  disabled,
  className,
  longLabel = false,
}: {
  kinds: readonly PeriodKind[];
  /** The art in effect, or null while the two ends are none. */
  value: PeriodKind | null;
  /**
   * Called for every pick, the art already in effect included — picking it again is how one jumps to the
   * current period (see [PeriodStepper]), and Radix would report no change for it.
   */
  onSelect: (kind: PeriodKind) => void;
  /**
   * Where given, a first "Eigener Zeitraum" entry that releases the art in effect for a free range. Absent
   * on a form, whose art is derived from the dates and freed by editing them (see the class comment).
   */
  onClear?: () => void;
  disabled?: boolean;
  className?: string;
  /**
   * Spell the art out in the trigger ("Jahr bis heute") rather than abbreviate it ("J→"). For the roomy
   * contexts — a filter popover with the stepper on a line of its own — where the two characters the form
   * grid squeezes it to would only be a riddle no width forces. The dropdown always spells it out anyway.
   */
  longLabel?: boolean;
}) {
  const t = useTranslations();
  if (!kinds.length) return null;

  const name = (kind: PeriodKind, short = false) =>
    t(
      // A label key can be a namespace in the bundle (`calendar.month` also parents the month names),
      // where the leaf is `<key>._`; asking next-intl for the bare key throws `INSUFFICIENT_PATH`.
      leafKeyOf(short ? kind.shortLabelKey : kind.labelKey, t.has),
      kind.labelArg == null ? {} : { arg0: kind.labelArg }
    );

  return (
    <Select
      // Empty rather than absent, so Radix falls back to the placeholder and "no art in effect" is
      // visible in the trigger itself.
      value={value?.id ?? ""}
      disabled={disabled}
      onValueChange={(id) => {
        if (id === CUSTOM_PERIOD_KIND) return onClear?.();
        const picked = kinds.find((kind) => kind.id === id);
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
        // fits on the line the two date boxes leave. Where it is spelled out (a filter with room) it
        // may grow to the label instead.
        className={cn("w-auto gap-0.5 px-1", className)}
      >
        {/* Children rather than the item's own text, which would be the spelled-out name: Radix falls
            back to them only while something is selected, so the placeholder below still shows. The
            placeholder names the control where there is room (a filter), and is only the icon where the
            form grid has none — there nothing but an art in effect is worth the width. */}
        <SelectValue
          placeholder={
            longLabel ? (
              <span className="text-muted-foreground">
                {t("duration.choose")}
              </span>
            ) : (
              <HugeiconsIcon
                icon={Timer01Icon}
                className="text-muted-foreground"
              />
            )
          }
        >
          {value && name(value, !longLabel)}
        </SelectValue>
      </SelectTrigger>
      <SelectContent>
        {onClear && value && (
          // Releases the art for a free range, keeping the two dates — see the class comment. First, so it
          // reads as "none of the below". Its value is no [PeriodKindId], so it never clashes with an art.
          <SelectItem value={CUSTOM_PERIOD_KIND}>
            {t("duration.custom")}
          </SelectItem>
        )}
        {kinds.map((kind) =>
          kind.id === value?.id ? (
            // The art already in effect: Radix reports no change for it, so the pick is taken from the
            // item's own events. All three are attached rather than Radix's mouse/other split, because
            // the call is idempotent — it sets the same period however often it arrives.
            <SelectItem
              key={kind.id}
              value={kind.id}
              title={kind.tooltipCurrentKey && t(kind.tooltipCurrentKey)}
              onPointerUp={() => onSelect(kind)}
              onClick={() => onSelect(kind)}
              onKeyDown={(event) => {
                if (event.key === "Enter" || event.key === " ") onSelect(kind);
              }}
            >
              {name(kind)}
            </SelectItem>
          ) : (
            <SelectItem key={kind.id} value={kind.id}>
              {name(kind)}
            </SelectItem>
          )
        )}
      </SelectContent>
    </Select>
  );
}
