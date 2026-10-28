"use client";

import { useTranslations } from "next-intl";
import { EntityAutocompleteField } from "@/components/shared/form/entity-autocomplete-field";

/**
 * The creditor account of the employee (travel expenses). Its own field because `konto` has no UIDataType
 * of its own and so isn't in the generated metadata (see creditor-invoice's AccountField).
 */
export function EmployeeKontoField({ className }: { className?: string }) {
  const t = useTranslations();
  return (
    <EntityAutocompleteField
      name="konto"
      // `fibu.employee.konto` is the parent of its tooltip, so the text itself is `._`.
      label={t("fibu.employee.konto._")}
      // The REST category of the accounts — `account`, not `konto` (see SEARCH_ENTITY).
      entity="account"
      metadataLess
      hint={t("fibu.employee.konto.tooltip")}
      className={className}
    />
  );
}
