"use client";

import { useId, useState } from "react";
import { useTranslations } from "next-intl";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import type { MassUpdateParameter } from "@/lib/rs/multi-select";

/**
 * The opt-in of the time sheet mass update that accepts the selected sheets booked by an ad hoc booking
 * grant proposed without the right to grant (`TimesheetMassUpdateRest.ACCEPT_PENDING`), together with the
 * task and cost unit picked above, if any. Sheets not to be accepted are left as they are by it.
 */
export function AcceptPendingCheckbox({
  setParam,
}: {
  setParam: (name: string, param: MassUpdateParameter | undefined) => void;
}) {
  const t = useTranslations();
  const id = useId();
  const [checked, setChecked] = useState(false);
  return (
    <div className="rounded-md border p-3">
      <div className="flex items-center gap-2">
        <Checkbox
          id={id}
          checked={checked}
          onCheckedChange={(value) => {
            setChecked(value === true);
            setParam(
              "acceptPending",
              value === true ? { change: true } : undefined
            );
          }}
        />
        <Label htmlFor={id} className="cursor-pointer font-medium">
          {t("timesheet.acceptance.accept")}
        </Label>
      </div>
      <p className="mt-2 text-xs text-muted-foreground">
        {t("timesheet.massupdate.acceptPending.info")}
      </p>
    </div>
  );
}
