"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { AlertCircleIcon } from "@hugeicons/core-free-icons";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { useKost2Check } from "../use-kost2-check";

/**
 * Says that a cost assignment books onto a cost unit outside the project — or, where the invoice names
 * none, outside the customer — the invoice is written for. Not an error: Wicket allows the combination
 * too, it only marks the field (`RechnungEditForm.onRenderCostRow` via `setWarningTooltip`).
 *
 * With a text, where Wicket has none: its tooltip helper sets a CSS class only, so the yellow outline
 * there says that something is off but never what. The sentence is one key
 * (`fibu.kost.error.kost2NotOfProject`), and it is what makes the mark actionable.
 */
export function Kost2Warning({ kost2Id }: { kost2Id?: number | null }) {
  const t = useTranslations();
  const matches = useKost2Check(kost2Id);
  if (matches) return null;

  return (
    <HintTooltip text={t("fibu.kost.error.kost2NotOfProject")}>
      {/* A button, like [FieldHint]: the explanation has to be reachable by keyboard and by tap, and an
          icon that only a mouse can read explains nothing on a phone. */}
      <button
        type="button"
        // `mt-6`: aligned with the box beside it, not with the label above it.
        className="-m-1 mt-6 shrink-0 p-1 text-warning transition-colors hover:text-warning/80 focus-visible:text-warning/80 focus-visible:outline-none"
        aria-label={t("fibu.kost.error.kost2NotOfProject")}
      >
        <HugeiconsIcon icon={AlertCircleIcon} size={14} strokeWidth={2} />
      </button>
    </HintTooltip>
  );
}
