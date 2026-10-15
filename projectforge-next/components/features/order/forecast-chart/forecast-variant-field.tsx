"use client";

import { useTranslations } from "next-intl";
import { Select, SelectTrigger } from "@/components/shared/copyable-select";
import { SelectItemWithHint } from "@/components/shared/form/select-item-with-hint";
import { Label } from "@/components/ui/label";
import { SelectContent, SelectValue } from "@/components/ui/select";
import { FORECAST_RUN_RATE_MIN_ELAPSED_MONTHS } from "@/lib/rs/order";

/**
 * The budget scenario of the forecast charts, on one line like the date fields beside it: optimistic
 * (unused budget distributed) or conservative (run rate, unused budget shown as difference and warned
 * about). Each option carries its explanation, in the wording of the Excel's Info sheet.
 */
export function ForecastVariantField({
  value,
  onChange,
}: {
  value: boolean;
  onChange: (value: boolean) => void;
}) {
  const t = useTranslations("fibu.auftrag.forecast.analysis.variants");
  const id = "forecastVariant";
  const label = t("column");
  return (
    <div className="flex items-center gap-2">
      <Label htmlFor={id} className="whitespace-nowrap">
        {label}:
      </Label>
      <Select
        value={String(value)}
        onValueChange={(it) => onChange(it === "true")}
      >
        <SelectTrigger id={id} className="h-8 w-auto" aria-label={label}>
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          <SelectItemWithHint
            value="true"
            hint={t("true._")}
            hintClassName="max-w-md"
          >
            {t("true.label")}
          </SelectItemWithHint>
          <SelectItemWithHint
            value="false"
            hint={t("false._", { arg0: FORECAST_RUN_RATE_MIN_ELAPSED_MONTHS })}
            hintClassName="max-w-md"
          >
            {t("false.label")}
          </SelectItemWithHint>
        </SelectContent>
      </Select>
    </div>
  );
}
