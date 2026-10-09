"use client";

import { useTranslations } from "next-intl";
import { NumberField } from "@/components/shared/form/number-field";
import { SelectField } from "@/components/shared/form/select-field";
import { cn } from "@/lib/utils";
import { useRuleValue } from "./use-rule-value";

/** The settings of the rule type (NotificationParams): only those of the chosen type are shown. */
export function RuleParamsField({ className }: { className?: string }) {
  const t = useTranslations("notification.params");
  const ruleType = useRuleValue("ruleType");
  if (ruleType === "TIMESHEETS_MISSING") {
    return (
      <SelectField
        name="referredMonth"
        label={t("referredMonth._")}
        metadataLess
        clearable={false}
        className={className}
        options={[
          { value: "CURRENT", label: t("referredMonth.CURRENT") },
          { value: "PREVIOUS", label: t("referredMonth.PREVIOUS") },
        ]}
      />
    );
  }
  if (ruleType === "VACATION_LEFT") {
    return (
      <div className={cn("grid gap-4 sm:grid-cols-2", className)}>
        <SelectField
          name="vacationExpiry"
          label={t("vacationExpiry._")}
          metadataLess
          clearable={false}
          options={[
            { value: "CARRY_OVER", label: t("vacationExpiry.CARRY_OVER") },
            { value: "YEAR_END", label: t("vacationExpiry.YEAR_END") },
          ]}
        />
        <NumberField
          name="daysBeforeExpiry"
          label={t("daysBeforeExpiry")}
          metadataLess
          fractionDigits={0}
          maxDigits={3}
        />
      </div>
    );
  }
  return null;
}
