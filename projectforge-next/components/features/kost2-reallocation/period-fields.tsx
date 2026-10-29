"use client";

import { useTranslations } from "next-intl";
import { Input } from "@/components/ui/input";
import { HintedLabel } from "./hinted-label";
import type { RunDraft } from "./types";

function NumberInput({
  label,
  hint,
  value,
  onChange,
  disabled,
  min,
  max,
}: {
  label: string;
  hint: string;
  value: number | null;
  onChange: (value: number | null) => void;
  disabled: boolean;
  min: number;
  max: number;
}) {
  return (
    <div className="flex w-32 flex-col gap-1">
      <HintedLabel label={label} hint={hint} />
      <Input
        type="number"
        min={min}
        max={max}
        value={value ?? ""}
        disabled={disabled}
        onChange={(e) =>
          onChange(e.target.value === "" ? null : Number(e.target.value))
        }
      />
    </div>
  );
}

/** The period of a run: first and last month (year and 1-based month). */
export function PeriodFields({
  draft,
  readOnly,
  onChange,
}: {
  draft: RunDraft;
  readOnly: boolean;
  onChange: (patch: Partial<RunDraft>) => void;
}) {
  const t = useTranslations();
  return (
    <div className="flex flex-wrap gap-4">
      <NumberInput
        label={t("fibu.kost2Reallocation.fromYear")}
        hint={t("fibu.kost2Reallocation.tooltip.periodFrom")}
        value={draft.fromYear}
        disabled={readOnly}
        min={2000}
        max={2100}
        onChange={(fromYear) => onChange({ fromYear })}
      />
      <NumberInput
        label={t("fibu.kost2Reallocation.fromMonth")}
        hint={t("fibu.kost2Reallocation.tooltip.periodFrom")}
        value={draft.fromMonth}
        disabled={readOnly}
        min={1}
        max={12}
        onChange={(fromMonth) => onChange({ fromMonth })}
      />
      <NumberInput
        label={t("fibu.kost2Reallocation.toYear")}
        hint={t("fibu.kost2Reallocation.tooltip.periodTo")}
        value={draft.toYear}
        disabled={readOnly}
        min={2000}
        max={2100}
        onChange={(toYear) => onChange({ toYear })}
      />
      <NumberInput
        label={t("fibu.kost2Reallocation.toMonth")}
        hint={t("fibu.kost2Reallocation.tooltip.periodTo")}
        value={draft.toMonth}
        disabled={readOnly}
        min={1}
        max={12}
        onChange={(toMonth) => onChange({ toMonth })}
      />
    </div>
  );
}
