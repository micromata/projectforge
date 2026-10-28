"use client";

import { useTranslations } from "next-intl";
import { ConflictNotice } from "@/components/shared/absence-chart/conflict-notice";
import { usePeriodClamp } from "@/components/shared/form/use-period-clamp";
import { useVacationStats } from "./use-vacation-stats";

/**
 * Above the form: the warning of a day without any replacement on duty (`vacation.conflict.info`), as it
 * follows the form. Also keeps the period a period (see usePeriodClamp), being rendered exactly
 * once per form.
 */
export function VacationConflictNotice() {
  const t = useTranslations();
  usePeriodClamp();
  const stats = useVacationStats();
  return (
    <ConflictNotice
      conflict={stats?.conflict}
      message={t("vacation.conflict.info")}
    />
  );
}
