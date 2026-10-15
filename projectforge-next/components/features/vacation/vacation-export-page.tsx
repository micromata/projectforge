"use client";

import { useState } from "react";
import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { useExportDownload } from "@/hooks/use-export-download";
import { useFormatContext } from "@/hooks/use-format";
import {
  periodKindOf,
  periodKindsOf,
  type Period,
  type PeriodKind,
} from "@/lib/date-period";
import { boundsOfPeriod, currentAnchorOf } from "@/lib/date-period-bounds";
import { isIsoDate } from "@/lib/date-period-math";
import { formatDateRange, type FormatContext } from "@/lib/format";
import { RsError } from "@/lib/rs/client";
import {
  downloadVacationExportExcel,
  fetchVacationExportInitial,
  fetchVacationExportView,
  VACATION_EXPORT_QUERY_KEY,
} from "@/lib/rs/vacation-export";
import { updateSearchParams } from "@/lib/search-params";
import type {
  VacationExportInitial,
  VacationExportRef,
  VacationExportRequest,
} from "./types";
import { VacationExportFilterRow } from "./vacation-export-filter-row";
import { VacationTeamChart } from "./vacation-team-chart";
import { chartWidthOf, type VacationChartWidth } from "./vacation-chart-width";
import { useVacationExportFavorites } from "./use-vacation-export-favorites";

/** The arts the team calendar pages through, the quarter first (the default). */
const KINDS = periodKindsOf(["quarter", "year"]);

/**
 * The team vacation calendar (`/next/vacationExport`), successor of the React page `react/vacationExport/dynamic`:
 * the vacations of the chosen employees and groups, shown per quarter or calendar year, and as Excel workbook
 * with a sheet per period (VacationExcelExporter) starting with the period shown. Optionally with the availability
 * entries of the same employees (those the user may see), in the chart and the workbook.
 *
 * The employees and groups chosen (and whether availabilities are shown) are stored by the backend (shown again on the next visit); the period is
 * kept in the url (`?period=quarter&from=2026-10-01`).
 *
 * Printed (the filter row's print button), it is the title, the period and the calendar shown: the choice
 * itself is not printed, the names are in the chart.
 */
export function VacationExportPage() {
  const t = useTranslations();
  const initial = useQuery({
    queryKey: [...VACATION_EXPORT_QUERY_KEY, "initial"],
    queryFn: ({ signal }) => fetchVacationExportInitial(signal),
    // It only seeds the page's state, once: a cached answer from an earlier visit would show (and with the first
    // view request store again) the choice and favorites of back then, over what was stored since. So it is
    // never kept for the next visit, and not fetched anew under a page already seeded.
    staleTime: 0,
    gcTime: 0,
    refetchOnWindowFocus: false,
  });
  return (
    <PageShell>
      <PageTitleRow
        category={t("menu.vacation._")}
        title={t("vacation.export.title")}
      />
      {/* Prints landscape (`@page vacation-calendar` in globals.css). */}
      <div
        className="flex flex-col gap-4 px-4 pb-6"
        data-print-page="vacation-calendar"
      >
        {initial.isPending && (
          <p className="text-sm text-muted-foreground">{t("loading")}</p>
        )}
        {initial.isError && (
          <p className="text-sm text-destructive">
            {initial.error instanceof RsError && initial.error.status === 403
              ? t("access.exception.noAccess")
              : t("errorpage.title")}
          </p>
        )}
        {initial.data && <VacationExportContent initial={initial.data} />}
      </div>
    </PageShell>
  );
}

function toRef(ref: VacationExportRef): EntityRef {
  return { id: ref.id, displayName: ref.displayName ?? "" };
}

/**
 * The period of the url, else the art stored with the choice (the current period of it), else this quarter. One
 * ending before `min` (an old link of a user who may not look into previous years) is the current one instead.
 */
function initialPeriod(
  params: URLSearchParams,
  storedKind: string | null | undefined,
  min: string | null,
  ctx: FormatContext
): Period {
  const offered = (id: string | null | undefined): PeriodKind | null => {
    const kind = periodKindOf(id);
    return kind && KINDS.includes(kind) ? kind : null;
  };
  const kind = offered(params.get("period")) ?? offered(storedKind) ?? KINDS[0];
  const from = params.get("from");
  // Only a day that exists: `2026-13-01` has the shape, but the backend can't read it.
  const anchor = isIsoDate(from)
    ? kind.beginOf(from, ctx)
    : currentAnchorOf(kind, ctx);
  return clampPeriod({ kind, anchor }, min, ctx);
}

/** The current period of the art instead of one ending before `min`. ISO dates compare as strings. */
function clampPeriod(
  period: Period,
  min: string | null,
  ctx: FormatContext
): Period {
  if (!min || period.kind.endOf(period.anchor, ctx) >= min) return period;
  return { kind: period.kind, anchor: currentAnchorOf(period.kind, ctx) };
}

function VacationExportContent({
  initial,
}: {
  initial: VacationExportInitial;
}) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const params = useSearchParams();
  const [employees, setEmployees] = useState<EntityRef[]>(() =>
    initial.employees.map(toRef)
  );
  const [groups, setGroups] = useState<EntityRef[]>(() =>
    initial.groups.map(toRef)
  );
  const [showAvailabilities, setShowAvailabilities] = useState(
    initial.showAvailabilities
  );
  const min = initial.earliestDate ?? null;
  const [dayWidth, setDayWidth] = useState<VacationChartWidth>(() =>
    chartWidthOf(initial.dayWidth)
  );
  const [period, setPeriod] = useState<Period>(() =>
    initialPeriod(
      new URLSearchParams(params.toString()),
      initial.periodKind,
      min,
      ctx
    )
  );
  const favorites = useVacationExportFavorites({
    initial: initial.favorites,
    employees,
    groups,
    apply: (nextEmployees, nextGroups) => {
      setEmployees(nextEmployees.map(toRef));
      setGroups(nextGroups.map(toRef));
    },
  });
  const bounds = boundsOfPeriod(period.kind, period.anchor, ctx);
  const body: VacationExportRequest = {
    employeeIds: employees.map((e) => e.id),
    groupIds: groups.map((g) => g.id),
    periodKind: period.kind.id,
    dayWidth,
    from: bounds.from,
    until: bounds.to,
    showAvailabilities,
  };
  const hasSelection = employees.length > 0 || groups.length > 0;

  // Also asked with nothing chosen: the backend stores the choice, an emptied one included.
  const view = useQuery({
    // With the width, though it only changes the drawing: asked again, the backend stores it at once.
    queryKey: [...VACATION_EXPORT_QUERY_KEY, "view", body],
    queryFn: ({ signal }) => fetchVacationExportView(body, signal),
    // The calendar stays while the next period or another choice loads.
    placeholderData: keepPreviousData,
    staleTime: 0,
  });
  const hasRows = (view.data?.rows.length ?? 0) > 0;
  const excel = useExportDownload(() => downloadVacationExportExcel(body));

  function selectPeriod(kind: PeriodKind, anchor: string) {
    const next = clampPeriod({ kind, anchor }, min, ctx);
    setPeriod(next);
    updateSearchParams({ period: next.kind.id, from: next.anchor }, "replace");
  }

  return (
    <>
      <VacationExportFilterRow
        kinds={KINDS}
        employees={employees}
        groups={groups}
        period={period}
        min={min}
        dayWidth={dayWidth}
        showAvailabilities={showAvailabilities}
        favorites={favorites}
        exporting={excel.isPending}
        canPrint={hasSelection && hasRows}
        onEmployeesChange={setEmployees}
        onGroupsChange={setGroups}
        onPeriodChange={selectPeriod}
        onDayWidthChange={setDayWidth}
        onShowAvailabilitiesChange={setShowAvailabilities}
        onExport={() => excel.mutate()}
      />
      <p className="hidden text-sm tabular-nums print:block">
        {formatDateRange(bounds.from, bounds.to, ctx)}
      </p>
      {!hasSelection && (
        <p className="text-sm text-muted-foreground">
          {t("vacation.export.selectHint")}
        </p>
      )}
      {hasSelection && view.isError && (
        <p className="text-sm text-destructive">{t("errorpage.title")}</p>
      )}
      {/* Also while the answer of the choice before stands in for this one and has nothing to show — the
          empty one of no choice at all, before the first pick: the area would stay blank until it comes. */}
      {hasSelection &&
        !view.isError &&
        (view.isPending || (view.isPlaceholderData && !hasRows)) && (
          <p className="text-sm text-muted-foreground">{t("loading")}</p>
        )}
      {hasSelection && view.data && hasRows && (
        <VacationTeamChart
          view={view.data}
          dayWidth={dayWidth}
          showAvailabilities={showAvailabilities}
        />
      )}
      {hasSelection && view.data && !hasRows && !view.isPlaceholderData && (
        <p className="text-sm text-muted-foreground">
          {t("datatable.no-records-found")}
        </p>
      )}
    </>
  );
}
