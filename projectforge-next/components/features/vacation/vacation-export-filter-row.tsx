"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { PrinterIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
import { EntityMultiAutocomplete } from "@/components/shared/entity-multi-autocomplete";
import { ExportButton } from "@/components/shared/export-button";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { FavoritesMenu } from "@/components/shared/favorites/favorites-menu";
import { PeriodStepper } from "@/components/shared/period-stepper";
import { useFormatContext } from "@/hooks/use-format";
import type { Period, PeriodKind } from "@/lib/date-period";
import { boundsOfPeriod } from "@/lib/date-period-bounds";
import { formatDateRange } from "@/lib/format";
import {
  VACATION_EXPORT_EMPLOYEES_URL,
  VACATION_EXPORT_GROUPS_URL,
} from "@/lib/rs/vacation-export";
import { VacationChartWidthSelect } from "./vacation-chart-width-select";
import type { ChartWidth } from "@/components/shared/absence-chart/absence-chart-width";
import type { useVacationExportFavorites } from "./use-vacation-export-favorites";

/**
 * What the team vacation calendar shows: the employees and groups (the lookups offer only what the user may
 * pick), the period — quarter or calendar year, paged with the arrows, not before `min` — the width of the day
 * columns, whether the availability entries are shown too (in the chart and the export), the Excel export of the
 * same choice and the print of the calendar shown (`canPrint`: one is shown). The favorites hold named choices of
 * employees and groups. Not printed itself (see VacationExportPage).
 */
export function VacationExportFilterRow({
  kinds,
  employees,
  groups,
  period,
  min,
  dayWidth,
  showAvailabilities,
  favorites,
  exporting,
  canPrint,
  onEmployeesChange,
  onGroupsChange,
  onPeriodChange,
  onDayWidthChange,
  onShowAvailabilitiesChange,
  onExport,
}: {
  kinds: readonly PeriodKind[];
  employees: EntityRef[];
  groups: EntityRef[];
  period: Period;
  min: string | null;
  dayWidth: ChartWidth;
  showAvailabilities: boolean;
  favorites: ReturnType<typeof useVacationExportFavorites>;
  exporting: boolean;
  canPrint: boolean;
  onEmployeesChange: (value: EntityRef[]) => void;
  onGroupsChange: (value: EntityRef[]) => void;
  onPeriodChange: (kind: PeriodKind, anchor: string) => void;
  onDayWidthChange: (value: ChartWidth) => void;
  onShowAvailabilitiesChange: (value: boolean) => void;
  onExport: () => void;
}) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const bounds = boundsOfPeriod(period.kind, period.anchor, ctx);
  const remove = (entry: EntityRef) => `${t("delete")}: ${entry.displayName}`;
  return (
    <div className="flex flex-wrap items-end gap-4 print:hidden">
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
            min={min}
            longLabel
          />
          <span className="whitespace-nowrap text-sm tabular-nums">
            {formatDateRange(bounds.from, bounds.to, ctx)}
          </span>
        </div>
      </div>
      <VacationChartWidthSelect value={dayWidth} onChange={onDayWidthChange} />
      <HintTooltip text={t("vacation.export.showAvailabilities.tooltip")}>
        <div className="flex h-8 items-center gap-2">
          <Checkbox
            id="vacation-export-show-availabilities"
            checked={showAvailabilities}
            onCheckedChange={(value) =>
              onShowAvailabilitiesChange(value === true)
            }
          />
          <Label
            htmlFor="vacation-export-show-availabilities"
            className="text-sm font-normal"
          >
            {t("vacation.export.showAvailabilities._")}
          </Label>
        </div>
      </HintTooltip>
      <FavoritesMenu
        favorites={favorites.favorites}
        currentId={favorites.currentId}
        isModified={favorites.isModified}
        onSelect={favorites.select}
        onCreate={favorites.create}
        onRename={favorites.rename}
        onUpdate={favorites.update}
        onDelete={favorites.remove}
        label={t("favorites._")}
        showLabel
      />
      <ExportButton
        label={t("exportAsXls")}
        tooltip={t("vacation.export.excel.tooltip")}
        isPending={exporting}
        disabled={employees.length === 0 && groups.length === 0}
        onClick={onExport}
      />
      <HintTooltip text={t("vacation.export.print.tooltip")}>
        {/* Wrapped while disabled, so the tooltip still shows (see ExportButton). */}
        <span tabIndex={canPrint ? undefined : 0}>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            className="gap-1.5"
            disabled={!canPrint}
            onClick={() => window.print()}
          >
            <HugeiconsIcon icon={PrinterIcon} size={14} aria-hidden />
            {t("print")}
          </Button>
        </span>
      </HintTooltip>
    </div>
  );
}
