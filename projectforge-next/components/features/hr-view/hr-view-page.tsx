"use client";

import { useState } from "react";
import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { fetchHrView } from "@/lib/rs/hr-view";
import { HrViewFilterRow } from "./hr-view-filter-row";
import { HrViewMatrix } from "./hr-view-matrix";
import { UnplannedUsers } from "./unplanned-users";
import type { HrViewQuery } from "./types";

/**
 * The HR view ("Personalplanung"), a hand-built standalone page: the planned and booked man days of the
 * employees in a period, one row per employee and one column per project or customer.
 *
 * The options drive a single query. Left out, they are the ones of the last visit (the backend stores them),
 * so the page opens with an empty query unless the deep-link `?startDay=&stopDay=` names a period. Once
 * loaded, the filter row shows and changes the effective options the view answered with.
 */
export function HrViewPage() {
  const t = useTranslations();
  const params = useSearchParams();
  const [query, setQuery] = useState<HrViewQuery>(() => ({
    startDay: params.get("startDay") ?? undefined,
    stopDay: params.get("stopDay") ?? undefined,
  }));

  const view = useQuery({
    queryKey: ["hrView", query],
    queryFn: ({ signal }) => fetchHrView(query, signal),
    // The table stays while the next period loads instead of collapsing to "loading".
    placeholderData: keepPreviousData,
  });
  const data = view.data;

  return (
    <PageShell>
      <PageTitleRow
        category={t("menu.projectmanagement")}
        title={t("menu.hrList")}
      />
      <div className="flex flex-col gap-4 px-4 pb-6">
        {data && (
          <HrViewFilterRow
            filter={data.filter}
            calendarWeeks={data.calendarWeeks}
            onChange={setQuery}
          />
        )}
        {view.isPending && (
          <p className="text-sm text-muted-foreground">{t("loading")}</p>
        )}
        {view.isError && (
          <p className="text-sm text-destructive">
            {t("access.exception.noAccess")}
          </p>
        )}
        {data && (
          <>
            <HrViewMatrix view={data} />
            {data.unplannedUsers.length > 0 && <UnplannedUsers view={data} />}
          </>
        )}
      </div>
    </PageShell>
  );
}
