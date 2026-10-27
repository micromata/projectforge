"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import type { ColumnDef } from "@tanstack/react-table";
import { DataTable } from "@/components/data-table";
import { Input } from "@/components/ui/input";
import type { Kost2ReallocationRule } from "@/lib/rs/kost2-reallocation";

/**
 * The rules A → B of a run: the records of the source cost unit go completely to the target. The account range is
 * optional; salary records are left to the time sheets unless the range names the salary account explicitly.
 */
export function RulesTable({
  rules,
  onChange,
  readOnly,
}: {
  rules: Kost2ReallocationRule[];
  onChange: (rules: Kost2ReallocationRule[]) => void;
  readOnly: boolean;
}) {
  const t = useTranslations();
  const columns = useMemo<ColumnDef<Kost2ReallocationRule, unknown>[]>(() => {
    const update = (index: number, patch: Partial<Kost2ReallocationRule>) =>
      onChange(rules.map((r, i) => (i === index ? { ...r, ...patch } : r)));
    const text = (
      id: string,
      label: string,
      cell: (rule: Kost2ReallocationRule) => string | undefined,
      size: number
    ): ColumnDef<Kost2ReallocationRule, unknown> => ({
      id,
      header: label,
      size,
      enableSorting: false,
      meta: { label },
      cell: ({ row }) => cell(row.original),
    });
    const konto = (
      id: "kontoFrom" | "kontoTo",
      labelKey: string
    ): ColumnDef<Kost2ReallocationRule, unknown> => ({
      id,
      header: t(labelKey),
      size: 130,
      enableSorting: false,
      meta: { label: t(labelKey) },
      cell: ({ row }) => (
        <Input
          type="number"
          className="h-7 w-28"
          aria-label={t(labelKey)}
          disabled={readOnly}
          value={row.original[id] ?? ""}
          onChange={(e) =>
            update(row.index, {
              [id]: e.target.value === "" ? null : Number(e.target.value),
            })
          }
        />
      ),
    });
    return [
      text(
        "source",
        t("fibu.kost2Reallocation.rule.source"),
        (r) => r.sourceKost2?.displayName,
        280
      ),
      text(
        "target",
        t("fibu.kost2Reallocation.rule.target"),
        (r) => r.targetKost2?.displayName,
        280
      ),
      konto("kontoFrom", "fibu.kost2Reallocation.rule.kontoFrom"),
      konto("kontoTo", "fibu.kost2Reallocation.rule.kontoTo"),
    ];
  }, [t, rules, onChange, readOnly]);

  return (
    <DataTable<Kost2ReallocationRule>
      columns={columns}
      data={rules}
      enableColumnFilters={false}
      manualSorting={false}
      getRowId={(_, index) => String(index)}
    />
  );
}
