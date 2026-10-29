"use client";

import { useMemo } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { DataTable } from "@/components/data-table";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { Button } from "@/components/ui/button";
import { useFormatContext } from "@/hooks/use-format";
import { formatTimestampMinutes } from "@/lib/format";
import { statusLabel } from "./status-label";
import {
  fetchKost2ReallocationRuns,
  type Kost2ReallocationRunRow,
} from "@/lib/rs/kost2-reallocation";

export const KOST2_REALLOCATION_RUNS_KEY = ["kost2Reallocation", "runs"];

const pad = (month?: number | null) => String(month ?? "").padStart(2, "0");

/** The runs of the Kost2 reallocation (finance only, enforced by the endpoints): a click on a row opens its wizard. */
export function Kost2ReallocationList() {
  const t = useTranslations();
  const format = useFormatContext();
  const router = useRouter();
  const query = useQuery({
    queryKey: KOST2_REALLOCATION_RUNS_KEY,
    queryFn: ({ signal }) => fetchKost2ReallocationRuns(signal),
  });

  const columns = useMemo<ColumnDef<Kost2ReallocationRunRow, unknown>[]>(() => {
    const col = (
      id: string,
      label: string,
      cell: (row: Kost2ReallocationRunRow) => React.ReactNode,
      size: number
    ): ColumnDef<Kost2ReallocationRunRow, unknown> => ({
      id,
      header: label,
      size,
      enableSorting: false,
      meta: { label },
      cell: ({ row }) => cell(row.original),
    });
    return [
      col("description", t("description"), (r) => r.description, 320),
      col(
        "projekt",
        t("fibu.kost2Reallocation.targetProject"),
        (r) => r.projekt?.displayName,
        260
      ),
      col(
        "period",
        t("fibu.kost2Reallocation.period"),
        (r) =>
          `${r.fromYear}-${pad(r.fromMonth)} – ${r.toYear}-${pad(r.toMonth)}`,
        160
      ),
      col("status", t("status"), (r) => statusLabel(t, r.status), 120),
      col(
        "appliedAt",
        t("fibu.kost2Reallocation.appliedAt"),
        (r) => formatTimestampMinutes(r.appliedAt, format),
        160
      ),
    ];
  }, [t, format]);

  return (
    <PageShell>
      <PageTitleRow title={t("fibu.kost2Reallocation.title")}>
        <Button asChild size="sm">
          <Link href="/kost2-reallocation/new">
            {t("fibu.kost2Reallocation.new")}
          </Link>
        </Button>
      </PageTitleRow>
      <div className="flex min-h-0 flex-1 flex-col">
        <DataTable<Kost2ReallocationRunRow>
          columns={columns}
          data={query.data ?? []}
          isFetching={query.isFetching}
          enableColumnFilters={false}
          manualSorting={false}
          getRowId={(row) => String(row.id)}
          onRowClick={(row) => router.push(`/kost2-reallocation/${row.id}`)}
        />
      </div>
    </PageShell>
  );
}
