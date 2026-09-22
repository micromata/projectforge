"use client";

import { useTranslations } from "next-intl";
import { SegmentedNumberField } from "@/components/shared/form/segmented-number-field";
import { kost1Segments } from "./cost-number-segments";

/**
 * The number of a cost unit: four boxes reading as one number, `6.100.01.02`.
 *
 * Labelled `fibu.kost.kostentraeger` like the fieldset of Wicket's edit form, and the boxes by the
 * `@PropertyInfo` keys of Kost1DO's own parts.
 */
export function CostNumberField({ className }: { className?: string }) {
  const t = useTranslations();
  const tKost1 = useTranslations("fibu.kost1");
  return (
    <SegmentedNumberField
      label={t("fibu.kost.kostentraeger")}
      segments={kost1Segments((name) => tKost1(name))}
      separator="."
      className={className}
    />
  );
}
