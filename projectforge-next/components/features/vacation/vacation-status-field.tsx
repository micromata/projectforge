"use client";

import { useTranslations } from "next-intl";
import {
  useEntityData,
  useFormReadOnly,
} from "@/components/shared/form/form-context";
import { SelectField } from "@/components/shared/form/select-field";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { VACATION_METADATA } from "@/lib/metadata/vacation.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import type { VacationDetail } from "./types";
import { useVacationStats } from "./use-vacation-stats";

const m = fromMetadata(VACATION_METADATA);

/**
 * The status, offering only what the user may set (`VacationDao.getAllowedStatus`): the employee can't
 * approve the own entry, a manager can't approve special leave, a past entry keeps its status. The list
 * follows the form (it depends on manager, special leave and the start date); the stored value is always
 * offered, so a status the user can't choose any more is shown rather than blanked.
 */
export function VacationStatusField({ className }: { className?: string }) {
  const t = useTranslations();
  const label = useFieldLabels(VACATION_METADATA);
  const readOnly = useFormReadOnly();
  const data = useEntityData<VacationDetail>();
  const stats = useVacationStats();
  const allowed = new Set<string>(stats?.allowedStatus ?? []);
  if (data?.status) allowed.add(data.status);
  return (
    <SelectField
      name="status"
      label={label("status")}
      disabled={readOnly}
      className={className}
      options={m.enumOptions("status", t).filter((o) => allowed.has(o.value))}
    />
  );
}
