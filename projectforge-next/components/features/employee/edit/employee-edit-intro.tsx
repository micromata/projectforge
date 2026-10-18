"use client";

import { useTranslations } from "next-intl";
import { FormAlert } from "@/components/shared/form-alert";
import { useEntityData } from "@/components/shared/form/form-context";
import type { EmployeeDetail } from "../types";

/**
 * For a new employee only: the status, weekly hours and annual leave can be entered once it is stored —
 * the form stays open after the first save for this (see the route's outcome).
 */
export function EmployeeEditIntro() {
  const t = useTranslations();
  const data = useEntityData<EmployeeDetail>();
  if (!data || data.id != null) return null;
  return <FormAlert tone="info">{t("fibu.employee.insert.hint")}</FormAlert>;
}
