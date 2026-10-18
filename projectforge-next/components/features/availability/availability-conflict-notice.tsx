"use client";

import { useTranslations } from "next-intl";
import { ConflictNotice } from "@/components/shared/absence-chart/conflict-notice";
import { usePeriodClamp } from "@/components/shared/form/use-period-clamp";
import { useAvailabilityStats } from "./use-availability-stats";

/**
 * Above the form: the warning of a day without any substitute on duty (`availability.conflict.info`), as it follows
 * the form. Also keeps the period a period (see usePeriodClamp), being rendered exactly once per form.
 */
export function AvailabilityConflictNotice() {
  const t = useTranslations();
  usePeriodClamp();
  const stats = useAvailabilityStats();
  return (
    <ConflictNotice
      conflict={stats?.conflict}
      message={t("availability.conflict.info")}
    />
  );
}
