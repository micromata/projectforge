"use client";

import { useTranslations } from "next-intl";
import { SegmentedNumberField } from "@/components/shared/form/segmented-number-field";
import { kost2Segments } from "./cost2-number-segments";

/**
 * The number of a cost unit: four boxes reading as one number, `6.100.01.02`.
 *
 * Labelled `fibu.kost.kostentraeger` like the fieldset of Wicket's edit form. The first three boxes
 * carry the generic cost labels; the last one is the `Kost2Art` (`fibu.kost2.art`) — a plain 0-99 box
 * in Wicket too, bound to `kost2Art.id` (the DTO carries it flat as `endziffer`).
 */
export function Cost2NumberField({ className }: { className?: string }) {
  const t = useTranslations();
  const label = (name: string) =>
    name === "endziffer" ? t("fibu.kost2.art") : t(`fibu.kost1.${name}`);
  return (
    <SegmentedNumberField
      label={t("fibu.kost.kostentraeger")}
      segments={kost2Segments(label)}
      separator="."
      className={className}
    />
  );
}
