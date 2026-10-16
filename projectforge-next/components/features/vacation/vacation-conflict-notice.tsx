"use client";

import { useTranslations } from "next-intl";
import { Alert, AlertDescription } from "@/components/ui/alert";
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
  if (stats?.conflict !== true) return null;
  return (
    <Alert variant="destructive" className="mx-6 mt-4 w-auto">
      <AlertDescription>{t("vacation.conflict.info")}</AlertDescription>
    </Alert>
  );
}
