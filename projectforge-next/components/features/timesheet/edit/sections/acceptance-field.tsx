"use client";

import { useTranslations } from "next-intl";
import { useEntityData } from "@/components/shared/form/form-context";
import { CheckboxField } from "@/components/shared/form/checkbox-field";
import { FormAlert } from "@/components/shared/form-alert";
import type { TimesheetDetail } from "../../types";

/**
 * For a sheet booked by an ad hoc booking grant proposed without the right to grant: it is to be accepted by
 * the project management, maybe on another task or cost unit. Who may accept it (`mayAccept`) ticks "accept
 * on save", the save then accepts it with the values of the form (`TimesheetEntityRest.accept`). Nothing
 * for any other sheet.
 */
export function AcceptanceField({ className }: { className?: string }) {
  const t = useTranslations();
  const acceptance = useEntityData<TimesheetDetail>()?.acceptance;
  if (!acceptance) return null;
  return (
    <div className={className}>
      <FormAlert tone="warning">
        <div className="flex flex-col gap-2">
          <span>
            {t("timesheet.acceptance.info", {
              arg0: acceptance.proposedByName ?? "?",
            })}
          </span>
          {acceptance.mayAccept && (
            <CheckboxField
              name="acceptOnSave"
              label={t("timesheet.acceptance.acceptOnSave")}
            />
          )}
        </div>
      </FormAlert>
    </div>
  );
}
