"use client";

import { useCallback, useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import { DataTable } from "@/components/data-table/data-table";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import type {
  ContributionMarginBreakdown,
  ContributionMarginBreakdownRow,
} from "@/lib/rs/order";
import { useBreakdownColumns } from "./contribution-margin-breakdown-columns";
import { useBreakdownRowLabel } from "./contribution-margin-breakdown-label";
import {
  expandableIds,
  initiallyExpanded,
  visibleRows,
} from "./contribution-margin-breakdown-rows";
import { ContributionMarginMonthlyChart } from "./contribution-margin-monthly-chart";

/**
 * The tree of the breakdown: the visible rows (all ancestors expanded) in a data table, expand and collapse
 * all, and the months of the selected row in the chart above.
 */
export function ContributionMarginBreakdownTree({
  data,
}: {
  data: ContributionMarginBreakdown;
}) {
  const t = useTranslations("fibu.auftrag.contributionMargin");
  const tc = useTranslations();
  const rowLabel = useBreakdownRowLabel();
  const [expanded, setExpanded] = useState(() => initiallyExpanded(data.rows));
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const toggle = useCallback(
    (id: string) =>
      setExpanded((current) => {
        const next = new Set(current);
        if (!next.delete(id)) next.add(id);
        return next;
      }),
    []
  );
  const columns = useBreakdownColumns(data, expanded, toggle);
  const rows = useMemo(
    () => visibleRows(data.rows, expanded),
    [data.rows, expanded]
  );
  const selected = data.rows.find((row) => row.id === selectedId);
  const chartTitle =
    selected && t("breakdown.chart", { arg0: rowLabel(selected) });
  return (
    <div className="space-y-4">
      {selected ? (
        <section className="space-y-2">
          <h4 className="text-sm font-semibold">{chartTitle}</h4>
          <ContributionMarginMonthlyChart
            data={{ ...data, ...selected.series }}
            ariaLabel={chartTitle}
          />
        </section>
      ) : (
        <p className="text-sm text-muted-foreground">
          {t("breakdown.selectRow")}
        </p>
      )}
      <div className="flex justify-end gap-2">
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={() => setExpanded(expandableIds(data.rows))}
        >
          {tc("expand")}
        </Button>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={() => setExpanded(new Set())}
        >
          {tc("collapse")}
        </Button>
      </div>
      <div className="flex max-h-[100cqh] flex-col">
        <DataTable<ContributionMarginBreakdownRow>
          columns={columns}
          data={rows}
          enableColumnFilters={false}
          manualSorting={false}
          manualPagination
          showPagination={false}
          columnLines
          getRowId={(row) => row.id}
          rowClassName={(row) =>
            cn(
              row.type === "PROJECT" && "font-medium",
              row.id === selectedId && "bg-muted"
            )
          }
          onRowClick={(row) =>
            setSelectedId((current) => (current === row.id ? null : row.id))
          }
        />
      </div>
    </div>
  );
}
