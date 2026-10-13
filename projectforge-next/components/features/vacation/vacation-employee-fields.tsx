"use client";

import { useTranslations } from "next-intl";
import { EntityAutocompleteField } from "@/components/shared/form/entity-autocomplete-field";
import { EntityMultiAutocompleteField } from "@/components/shared/form/entity-multi-autocomplete-field";
import {
  useEntityData,
  useFormReadOnly,
} from "@/components/shared/form/form-context";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { VACATION_METADATA } from "@/lib/metadata/vacation.generated";
import type { VacationDetail } from "./types";

/**
 * Whose vacation it is. Only HR staff may choose (`Vacation.employeeEditable`); everybody else enters their
 * own one and sees the name only, as on the React form.
 */
export function VacationEmployeeField({ className }: { className?: string }) {
  const label = useFieldLabels(VACATION_METADATA);
  const data = useEntityData<VacationDetail>();
  const readOnly = useFormReadOnly();
  return (
    <EntityAutocompleteField
      name="employee"
      label={label("employee")}
      entity="employee"
      disabled={readOnly || data?.employeeEditable !== true}
      className={className}
    />
  );
}

/**
 * Further replacements beside the main one; a conflict exists only if none of them is on duty. Custom
 * because a collection of references has no field in the metadata.
 */
export function VacationOtherReplacementsField({
  className,
}: {
  className?: string;
}) {
  const t = useTranslations();
  return (
    <EntityMultiAutocompleteField
      name="otherReplacements"
      label={t("vacation.replacement.others")}
      entity="employee"
      metadataLess
      className={className}
    />
  );
}
