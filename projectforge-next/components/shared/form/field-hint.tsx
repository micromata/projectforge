"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { InformationCircleIcon } from "@hugeicons/core-free-icons";
import { HintTooltip } from "@/components/shared/hint-tooltip";

/**
 * The explanation of a field, behind an icon next to its label.
 *
 * Not a line under the control ([FieldDescription], which is what this replaced): the hints are whole
 * sentences ("When will the sales be invoiced? Does not apply to payment schedule.") and printing them
 * under every field they belong to pushes the form apart until the fields themselves are the minority
 * of what is on the page. As a tooltip the sentence is one hover away and the row keeps its height.
 *
 * Also reachable by keyboard and by tap: the trigger is a `<button>`, which Radix opens on focus and
 * on press as well as on hover — an explanation only the mouse can read is no explanation on a phone.
 */
export function FieldHint({ hint, label }: { hint: string; label: string }) {
  const t = useTranslations("form");
  return (
    <HintTooltip text={hint}>
      <button
        type="button"
        // `-m-1 p-1`: a target big enough to hit without making the label line taller or wider.
        className="-m-1 shrink-0 p-1 text-muted-foreground/70 transition-colors hover:text-foreground focus-visible:text-foreground focus-visible:outline-none"
        aria-label={`${t("hint")}: ${label}`}
      >
        <HugeiconsIcon icon={InformationCircleIcon} size={13} strokeWidth={2} />
      </button>
    </HintTooltip>
  );
}
