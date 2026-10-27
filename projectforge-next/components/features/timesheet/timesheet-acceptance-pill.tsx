"use client";

import { useTranslations } from "next-intl";
import { StatusPill } from "@/components/shared/status-pill";

/** Marks a time sheet still to be accepted by the project management (booked by a proposed grant). */
export function TimesheetAcceptancePill() {
  const t = useTranslations();
  return <StatusPill tone="info" label={t("timesheet.acceptancePending")} />;
}
