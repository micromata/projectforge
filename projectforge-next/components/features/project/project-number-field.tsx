"use client";

import { useMemo } from "react";
import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { SegmentedNumberField } from "@/components/shared/form/segmented-number-field";
import type { NumberSegment } from "@/lib/form/number-segments";
import type { ProjectValues } from "./project-schema";

/**
 * The project's cost number, as Wicket's `CostNumber24Component` shows it: `5.123.04.##` for a project
 * of customer 123, `4.456.04.##` for an internal one. The number range and the range (Bereich) follow
 * from the customer — only the project's two digits, and for an internal project its range, are the
 * user's to type. `.##` stands for the cost 2 type, which each cost 2 unit of the project adds.
 *
 * Reads the customer live, so the number re-shapes the moment one is picked or cleared.
 *
 * Fixed once the project has cost 2 units, which carry a copy of the number (`numberLocked`, guarded
 * by `ProjektDao.onUpdate`); so is ProjectCustomerField.
 */
export function ProjectNumberField({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const customerId = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state) => ((state as any).values as ProjectValues).customer?.id ?? null
  );
  const hasCustomer = customerId != null;
  const locked = useNumberLocked();

  const segments = useMemo<NumberSegment[]>(() => {
    const nummer: NumberSegment = {
      name: "nummer",
      label: t("fibu.projekt.nummer"),
      min: 0,
      max: 99,
      digits: 2,
    };
    if (hasCustomer) return [nummer];
    return [
      {
        name: "internKost2_4",
        label: t("fibu.projekt.internKost2_4"),
        min: 0,
        max: 999,
        digits: 3,
      },
      nummer,
    ];
  }, [hasCustomer, t]);

  const prefix =
    customerId != null ? `5.${String(customerId).padStart(3, "0")}.` : "4.";

  return (
    <SegmentedNumberField
      label={t("fibu.projekt.nummer")}
      segments={segments}
      separator="."
      prefix={prefix}
      suffix=".##"
      disabled={locked}
      hint={locked ? t("fibu.projekt.validation.numberLocked") : undefined}
      className={className}
    />
  );
}

/** Whether number and customer are fixed (see ProjectNumberField); shared with ProjectCustomerField. */
export function useNumberLocked(): boolean {
  const form = useEntityEditForm();
  return useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state) => ((state as any).values as ProjectValues).numberLocked
  );
}
