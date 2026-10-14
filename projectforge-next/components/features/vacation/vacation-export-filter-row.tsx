"use client";

import { useTranslations } from "next-intl";
import { Label } from "@/components/ui/label";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
import { EntityMultiAutocomplete } from "@/components/shared/entity-multi-autocomplete";
import { ExportButton } from "@/components/shared/export-button";
import { PeriodStepper } from "@/components/shared/period-stepper";
import { useFormatContext } from "@/hooks/use-format";
import type { Period, PeriodKind } from "@/lib/date-period";
import { boundsOfPeriod } from "@/lib/date-period-bounds";
import { formatDateRange } from "@/lib/format";
import {
  VACATION_EXPORT_EMPLOYEES_URL,
  VACATION_EXPORT_GROUPS_URL,
} from "@/lib/rs/vacation-export";

/**
 * What the team vacation calendar shows: the employees and groups (the lookups offer only what the user may
 * pick), the period — quarter or calendar year, paged with the arrows — and the Excel export of the same choice.
 */
export function VacationExportFilterRow({
  kinds,
  employees,
  groups,
  period,
  exporting,
  onEmployeesChange,
  onGroupsChange,
  onPeriodChange,
  onExport,
}: {
  kinds: readonly PeriodKind[];
  employees: EntityRef[];
  groups: EntityRef[];
  period: Period;
  exporting: boolean;
  onEmployeesChange: (value: EntityRef[]) => void;
  onGroupsChange: (value: EntityRef[]) => void;
  onPeriodChange: (kind: PeriodKind, anchor: string) => void;
  onExport: () => void;
}) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const bounds = boundsOfPeriod(period.kind, period.anchor, ctx);
  const remove = (entry: EntityRef) => `${t("delete")}: ${entry.displayName}`;
  return (
    <div className="flex flex-wrap items-end gap-4">
      <div className="flex min-w-64 flex-1 flex-col gap-1.5">
        <Label htmlFor="vacation-export-employees">{t("fibu.employees")}</Label>
        <EntityMultiAutocomplete
          id="vacation-export-employees"
          url={VACATION_EXPORT_EMPLOYEES_URL}
          value={employees}
          onChange={onEmployeesChange}
          removeLabel={remove}
          sorted
        />
      </div>
      <div className="flex min-w-64 flex-1 flex-col gap-1.5">
        <Label htmlFor="vacation-export-groups">{t("group.groups")}</Label>
        <EntityMultiAutocomplete
          id="vacation-export-groups"
          url={VACATION_EXPORT_GROUPS_URL}
          value={groups}
          onChange={onGroupsChange}
          removeLabel={remove}
          sorted
        />
      </div>
      <div className="flex flex-col gap-1.5">
        <span className="text-sm font-medium">{t("filter.period")}</span>
        <div className="flex items-center gap-2">
          <PeriodStepper
            kinds={kinds}
            current={period}
            onSelect={onPeriodChange}
            longLabel
          />
          <span className="whitespace-nowrap text-sm tabular-nums">
            {formatDateRange(bounds.from, bounds.to, ctx)}
          </span>
        </div>
      </div>
      <ExportButton
        label={t("exportAsXls")}
        tooltip={t("vacation.export.excel.tooltip")}
        isPending={exporting}
        disabled={employees.length === 0 && groups.length === 0}
        onClick={onExport}
      />
    </div>
  );
}
