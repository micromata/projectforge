"use client";

import { useTranslations } from "next-intl";
import { Label } from "@/components/ui/label";
import { FieldShell, useFieldIds } from "@/components/shared/form/field-shell";
import { PersonSelect } from "@/components/shared/person-select";
import { VacationStatisticsTable } from "./vacation-statistics-table";
import type { VacationAccount } from "./types";

/**
 * The head of the vacation account, laid out as on the React page: the employee picker (HR staff only) on the
 * left, the leave statistics and the working days on the right, each only as wide as it needs to be.
 */
export function VacationAccountSummary({
  account,
  onSelectEmployee,
}: {
  account: VacationAccount;
  onSelectEmployee: (id: number) => void;
}) {
  const t = useTranslations();
  const ids = useFieldIds();
  const employee = account.employee;

  return (
    <div className="flex flex-wrap items-start gap-x-8 gap-y-4 pt-2">
      {account.hrAccess && (
        <div className="flex w-full flex-col gap-1.5 sm:w-72">
          <Label htmlFor="vacation-account-employee">
            {t("fibu.employee._")}
          </Label>
          <PersonSelect
            id="vacation-account-employee"
            kind="employee"
            required
            value={
              employee
                ? { id: employee.id, displayName: employee.displayName ?? "" }
                : null
            }
            onChange={(picked) => picked?.id && onSelectEmployee(picked.id)}
          />
        </div>
      )}
      {employee && (
        <div className="flex w-fit max-w-full flex-col gap-3">
          <VacationStatisticsTable account={account} />
          {account.workingHoursStatistics && (
            <FieldShell
              label={t("fibu.common.workingDays")}
              readOnly
              invalid={false}
              errors={[]}
              ids={ids}
            >
              <div
                id={ids.controlId}
                aria-labelledby={ids.labelId}
                className="flex min-h-7 items-center rounded-md border border-input bg-input/20 px-2 py-1 text-xs/relaxed text-muted-foreground dark:bg-input/30"
              >
                {account.workingHoursStatistics}
              </div>
            </FieldShell>
          )}
        </div>
      )}
    </div>
  );
}
