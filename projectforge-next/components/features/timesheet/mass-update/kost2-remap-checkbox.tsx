"use client";

import { useId } from "react";
import { useTranslations } from "next-intl";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";

/**
 * The opt-in of the time sheet mass update that remaps every sheet's cost unit to the one of the same type
 * its *own* task offers, leaving the task untouched (`TimesheetMultiSelectedPageRest.remapKost2`) — the fix
 * for sheets booked before the task's cost units changed, e.g. after a project was linked to it.
 */
export function Kost2RemapCheckbox({
  checked,
  onCheckedChange,
}: {
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
}) {
  const t = useTranslations();
  const id = useId();
  return (
    <div className="rounded-md border p-3">
      <div className="flex items-center gap-2">
        <Checkbox
          id={id}
          checked={checked}
          onCheckedChange={(value) => onCheckedChange(value === true)}
        />
        <Label htmlFor={id} className="cursor-pointer font-medium">
          {t("timesheet.massupdate.remapKost2")}
        </Label>
      </div>
      <p className="mt-2 text-xs text-muted-foreground">
        {t("timesheet.massupdate.remapKost2Info")}
      </p>
    </div>
  );
}
