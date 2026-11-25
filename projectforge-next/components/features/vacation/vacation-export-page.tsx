"use client";

import { useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { useExportDownload } from "@/hooks/use-export-download";
import { useFormatContext } from "@/hooks/use-format";
import {
  CUSTOM_PERIOD_KIND,
  periodKindsOf,
  type PeriodKind,
} from "@/lib/date-period";
import { formatDateRange } from "@/lib/format";
import {
  ABSENCE_KINDS,
  sortAbsenceKinds,
  type AbsenceKind,
} from "@/lib/rs/absence-kinds";
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
import {
  clampToMin,
  customPeriodOf,
  initialExportPeriod,
  periodOfKind,
  stepCustomPeriod,
  type ExportPeriod,
} from "./vacation-export-period";
import { VacationExportFilterRow } from "./vacation-export-filter-row";
import { VacationTeamChart } from "./vacation-team-chart";
import {
  chartWidthOf,
  type ChartWidth,
} from "@/components/shared/absence-chart/absence-chart-width";
import { useVacationExportFavorites } from "./use-vacation-export-favorites";
import { toPickerRef } from "@/lib/page-def/reference";

/** The arts the team calendar pages through, the quarter first (the default). */
const KINDS = periodKindsOf(["quarter", "year"]);

/**
 * The team vacation calendar (`/next/vacationExport`), successor of the React page `react/vacationExport/dynamic`:
 * the vacations of the chosen users and groups, shown per quarter, calendar year or any range of days, and as Excel workbook
 * with a sheet per period (VacationExcelExporter) starting with the period shown. Optionally with the availability
 * entries of the same users, as their vacations, in the chart and the workbook.
 *
 * The users and groups chosen and whether availabilities are shown are stored by the backend (shown again on
 * the next visit); the period is kept in the url (`?period=quarter&from=2026-10-01`, a free range as
 * `?period=custom&from=2026-11-01&until=2027-01-31`).
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
  return toPickerRef(ref)!;
}

/**
 * The kinds of the url: `all` (linked from the availability list), a comma separated list or the former
 * `?showAvailabilities=true` (all of them); null if it asks for none.
 */
function absenceKindsOfUrl(params: URLSearchParams): AbsenceKind[] | null {
  const value = params.get("absenceKinds");
  if (value === "all" || params.get("showAvailabilities") === "true") {
    return [...ABSENCE_KINDS];
  }
  const kinds = sortAbsenceKinds(value?.split(","));
  return kinds.length > 0 ? kinds : null;
}

function VacationExportContent({
  initial,
}: {
  initial: VacationExportInitial;
}) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const params = useSearchParams();
  const [users, setUsers] = useState<EntityRef[]>(() =>
    initial.users.map(toRef)
  );
  const [groups, setGroups] = useState<EntityRef[]>(() =>
    initial.groups.map(toRef)
  );
  // `?absenceKinds=all` (the availability list) or a list of kinds, otherwise the stored choice.
  const [absenceKinds, setAbsenceKinds] = useState<AbsenceKind[]>(
    () => absenceKindsOfUrl(params) ?? sortAbsenceKinds(initial.absenceKinds)
  );
  // Applied once: kept in the url, a reload or going back would apply it again against a choice made here.
  useEffect(() => {
    if (
      params.get("absenceKinds") !== null ||
      params.get("showAvailabilities") !== null
    ) {
      updateSearchParams(
        { absenceKinds: null, showAvailabilities: null },
        "replace"
      );
    }
    // Only on mount: the parameter is read by the initial state above.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  const min = initial.earliestDate ?? null;
  const [dayWidth, setDayWidth] = useState<ChartWidth>(() =>
    chartWidthOf(initial.dayWidth)
  );
  const [period, setPeriod] = useState<ExportPeriod>(() =>
    initialExportPeriod(
      new URLSearchParams(params.toString()),
      initial.periodKind,
      KINDS,
      min,
      ctx
    )
  );
  // Whether the end typed in was cut to what the backend answers for: told until the period changes again.
  const [shortened, setShortened] = useState(false);
  const favorites = useVacationExportFavorites({
    initial: initial.favorites,
    users,
    groups,
    apply: (nextUsers, nextGroups) => {
      setUsers(nextUsers.map(toRef));
      setGroups(nextGroups.map(toRef));
    },
  });
  const body: VacationExportRequest = {
    userIds: users.map((e) => e.id),
    groupIds: groups.map((g) => g.id),
    periodKind: period.kind?.id ?? CUSTOM_PERIOD_KIND,
    dayWidth,
    from: period.from,
    until: period.until,
    absenceKinds,
  };
  const hasSelection = users.length > 0 || groups.length > 0;

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

  function applyPeriod(next: ExportPeriod, wasShortened = false) {
    setPeriod(next);
    setShortened(wasShortened);
    updateSearchParams(
      next.kind
        ? { period: next.kind.id, from: next.from, until: null }
        : { period: CUSTOM_PERIOD_KIND, from: next.from, until: next.until },
      "replace"
    );
  }

  /** A whole period of an art, by picking the art or paging it. */
  function selectPeriod(kind: PeriodKind, anchor: string) {
    applyPeriod(clampToMin(periodOfKind(kind, anchor, ctx), min, ctx));
  }

  /** A free range: "Eigener Zeitraum" (keeping the two days) or one of the two days typed in. */
  function selectCustom(from: string, until: string) {
    const next = customPeriodOf(from, until, min);
    applyPeriod(next.period, next.shortened);
  }

  function stepCustom(steps: number) {
    const next = stepCustomPeriod(period, steps, min, ctx);
    if (next) applyPeriod(next);
  }

  return (
    <>
      <VacationExportFilterRow
        kinds={KINDS}
        users={users}
        groups={groups}
        period={period}
        min={min}
        dayWidth={dayWidth}
        absenceKinds={absenceKinds}
        favorites={favorites}
        exporting={excel.isPending}
        canPrint={hasSelection && hasRows}
        onUsersChange={setUsers}
        onGroupsChange={setGroups}
        shortened={shortened}
        onPeriodChange={selectPeriod}
        onCustomPeriodChange={selectCustom}
        onCustomStep={stepCustom}
        onDayWidthChange={setDayWidth}
        onAbsenceKindsChange={setAbsenceKinds}
        onExport={() => excel.mutate()}
      />
      <p className="hidden text-sm tabular-nums print:block">
        {formatDateRange(period.from, period.until, ctx)}
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
          absenceKinds={absenceKinds}
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
