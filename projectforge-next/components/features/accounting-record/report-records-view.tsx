"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { DataTable } from "@/components/data-table";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency } from "@/lib/format";
import { leafKeyOf } from "@/lib/leaf-key";
import { BUCHUNGSSATZ_METADATA } from "@/lib/metadata/buchungssatz.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import { fetchReportRecords } from "@/lib/rs/accounting-record";
import { AccountingRecordBwa } from "./accounting-record-bwa";
import type { AccountingRecordListRow } from "./types";

const m = fromMetadata(BUCHUNGSSATZ_METADATA);

/**
 * The report drill-down of the accounting records: the fixed set of bookings behind a report (or one of
 * its BWA rows), taken from the per-user in-memory `ReportStorage` rather than a DB query, plus that set's
 * own BWA. Entered from the (still-Wicket) Reporting UI as `?reportId=…[&businessAssessmentRowId=…]`; the
 * normal list filter is hidden because the set is fixed.
 *
 * The table is the shared {@link DataTable} (never a hand-rolled one) with the same columns the list shows;
 * it does not page or filter — the record set is small and complete.
 */
export function ReportRecordsView({
  reportId,
  businessAssessmentRowId,
}: {
  reportId: string;
  businessAssessmentRowId?: string | null;
}) {
  const t = useTranslations();
  const format = useFormatContext();

  const query = useQuery({
    queryKey: ["accountingRecord", "report", reportId, businessAssessmentRowId],
    queryFn: ({ signal }) =>
      fetchReportRecords(reportId, businessAssessmentRowId, signal),
  });

  const shLabels = useMemo(() => {
    const map = new Map<string, string>();
    for (const option of m.enumOptions("sh", t)) {
      map.set(String(option.value), option.label);
    }
    return map;
  }, [t]);

  const columns = useMemo<ColumnDef<AccountingRecordListRow, unknown>[]>(() => {
    const text = (
      id: string,
      labelKey: string,
      accessor: (row: AccountingRecordListRow) => string,
      size: number
    ): ColumnDef<AccountingRecordListRow, unknown> => {
      const label = t(leafKeyOf(labelKey, t.has));
      return {
        id,
        header: label,
        size,
        enableSorting: false,
        meta: { label },
        cell: ({ row }) => accessor(row.original),
      };
    };
    return [
      {
        id: "satznr",
        header: t("fibu.buchungssatz.satznr"),
        size: 130,
        enableSorting: false,
        meta: { label: t("fibu.buchungssatz.satznr") },
        cell: ({ row }) => (
          <span className="font-mono font-semibold">
            {row.original.satznr ?? ""}
          </span>
        ),
      },
      {
        id: "betrag",
        header: t("fibu.common.betrag"),
        size: 120,
        enableSorting: false,
        meta: { label: t("fibu.common.betrag"), align: "right" },
        cell: ({ row }) => (
          <span className="tabular-nums">
            {formatCurrency(row.original.betrag, format)}
          </span>
        ),
      },
      text("beleg", "fibu.buchungssatz.beleg", (r) => r.beleg ?? "", 110),
      text("kost1", "fibu.kost1", (r) => r.kost1?.displayName ?? "", 150),
      text("kost2", "fibu.kost2", (r) => r.kost2?.displayName ?? "", 150),
      text(
        "konto",
        "fibu.buchungssatz.konto",
        (r) => r.konto?.displayName ?? "",
        160
      ),
      text(
        "gegenKonto",
        "fibu.buchungssatz.gegenKonto",
        (r) => r.gegenKonto?.displayName ?? "",
        160
      ),
      text(
        "sh",
        "finance.accountingRecord.dc",
        (r) => (r.sh ? (shLabels.get(r.sh) ?? r.sh) : ""),
        80
      ),
      text("text", "fibu.buchungssatz.text", (r) => r.text ?? "", 260),
      text("comment", "comment", (r) => r.comment ?? "", 220),
    ];
  }, [t, format, shLabels]);

  const records = query.data?.records ?? [];

  return (
    <PageShell>
      <PageTitleRow title={t("fibu.buchungssatz.title.list")} />
      <AccountingRecordBwa
        statistics={query.data?.statistics ?? undefined}
        isFetching={query.isFetching}
      />
      <div className="flex min-h-0 flex-1 flex-col">
        <DataTable<AccountingRecordListRow>
          columns={columns}
          data={records}
          isFetching={query.isFetching}
          enableColumnFilters={false}
          manualSorting={false}
          getRowId={(row) => String(row.id)}
        />
      </div>
    </PageShell>
  );
}
