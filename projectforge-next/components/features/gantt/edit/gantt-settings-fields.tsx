"use client";

import { useTranslations } from "next-intl";
import { CheckboxField } from "@/components/shared/form/checkbox-field";
import { InputField } from "@/components/shared/form/input-field";
import { NumberField } from "@/components/shared/form/number-field";
import { SelectField } from "@/components/shared/form/select-field";
import { cn } from "@/lib/utils";

/**
 * The fields of the chart's settings and style (GanttChartSettings, GanttChartStyle) — custom fields, as
 * neither is a property of GanttChartDO with generated metadata (hence `metadataLess`); their labels are
 * the Wicket form's (GanttChartEditForm).
 */

export function GanttTitleField({ className }: { className?: string }) {
  const t = useTranslations();
  return (
    <InputField
      name="title"
      label={t("title")}
      className={className}
      metadataLess
    />
  );
}

function AccessField({
  name,
  labelKey,
  className,
}: {
  name: "readAccessType" | "writeAccessType";
  labelKey: string;
  className?: string;
}) {
  const t = useTranslations();
  const options = [
    { value: "OWNER", label: t("gantt.access.owner") },
    { value: "PROJECT_MANAGER", label: t("gantt.access.projectmanager") },
    { value: "ALL", label: t("gantt.access.all") },
  ];
  return (
    <SelectField
      name={name}
      label={t(labelKey)}
      options={options}
      clearable={false}
      className={className}
      metadataLess
    />
  );
}

export function GanttReadAccessField({ className }: { className?: string }) {
  return (
    <AccessField
      name="readAccessType"
      labelKey="access.read"
      className={className}
    />
  );
}

export function GanttWriteAccessField({ className }: { className?: string }) {
  return (
    <AccessField
      name="writeAccessType"
      labelKey="access.write"
      className={className}
    />
  );
}

export function GanttWidthField({ className }: { className?: string }) {
  const t = useTranslations();
  return (
    <NumberField
      name="width"
      label={t("gantt.settings.width")}
      fractionDigits={0}
      maxDigits={5}
      className={className}
      metadataLess
    />
  );
}

export function GanttLabelWidthField({ className }: { className?: string }) {
  const t = useTranslations();
  return (
    <NumberField
      name="totalLabelWidth"
      label={t("gantt.settings.totalLabelWidth")}
      fractionDigits={0}
      maxDigits={5}
      className={className}
      metadataLess
    />
  );
}

/** The chart's period; either end left empty is calculated from the activities. */
export function GanttPeriodField({ className }: { className?: string }) {
  const t = useTranslations();
  return (
    <div className={cn("flex min-w-0 flex-wrap gap-3", className)}>
      <InputField
        name="fromDate"
        type="date"
        label={`${t("timePeriod")} ${t("date.from")}`}
        className="min-w-36 flex-1"
        metadataLess
      />
      <InputField
        name="toDate"
        type="date"
        label={t("date.until")}
        className="min-w-36 flex-1"
        metadataLess
      />
    </div>
  );
}

/** Wicket's "Options" checkbox group: the flags of the style, and whether only visible rows are listed. */
export function GanttOptionsField({ className }: { className?: string }) {
  const t = useTranslations();
  return (
    <fieldset className={cn("flex min-w-0 flex-col gap-2", className)}>
      <legend className="mb-1 text-[11.5px] font-semibold uppercase tracking-wide text-muted-foreground">
        {t("label.options")}
      </legend>
      <CheckboxField
        name="relativeTimeValues"
        label={t("gantt.style.relativeTimeValues")}
      />
      <CheckboxField name="showToday" label={t("gantt.style.showToday")} />
      <CheckboxField
        name="showCompletion"
        label={t("gantt.style.showCompletion")}
      />
      <CheckboxField
        name="showOnlyVisibles"
        label={t("gantt.settings.showOnlyVisibles")}
      />
    </fieldset>
  );
}
