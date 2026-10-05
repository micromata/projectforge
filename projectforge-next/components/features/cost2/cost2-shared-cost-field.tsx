"use client";

import { useTranslations } from "next-intl";
import { SelectField } from "@/components/shared/form/select-field";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { KOST2_METADATA } from "@/lib/metadata/kost2.generated";

/**
 * Whether time sheets booked on this cost unit may overlap with those of other projects (Kost2DO.sharedCost).
 * Three states, so not a checkbox: Yes/No override the structure element's setting
 * (TaskDO.allowTimeOverlap), the cleared value leaves the decision to it — shown as the placeholder.
 */
export function Cost2SharedCostField({ className }: { className?: string }) {
  const t = useTranslations();
  const label = useFieldLabels(KOST2_METADATA);
  return (
    <SelectField
      name="sharedCost"
      label={label("sharedCost")}
      hint={t("fibu.kost2.sharedCost.tooltip")}
      className={className}
      options={[
        { value: "true", label: t("yes") },
        { value: "false", label: t("no") },
      ]}
      valueType="boolean"
      clearable
      placeholder={t("fibu.kost2.sharedCost.inherited")}
    />
  );
}
