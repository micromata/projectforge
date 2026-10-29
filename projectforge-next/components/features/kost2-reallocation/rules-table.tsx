"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import type { ColumnDef } from "@tanstack/react-table";
import { DataTable } from "@/components/data-table";
import { HintTooltip } from "@/components/shared/hint-tooltip";
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
    const header = (label: string, hint: string) => (
      <HintTooltip text={hint} openOnTap>
        <span className="cursor-help underline decoration-dotted underline-offset-4">
          {label}
        </span>
      </HintTooltip>
    );
    const text = (
      id: string,
      label: string,
      hint: string,
      cell: (rule: Kost2ReallocationRule) => string | undefined,
      size: number
    ): ColumnDef<Kost2ReallocationRule, unknown> => ({
      id,
      header: () => header(label, hint),
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
      header: () =>
        header(t(labelKey), t("fibu.kost2Reallocation.tooltip.ruleKonto")),
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
        t("fibu.kost2Reallocation.tooltip.ruleSource"),
        (r) => r.sourceKost2?.displayName,
        280
      ),
      text(
        "target",
        t("fibu.kost2Reallocation.rule.target"),
        t("fibu.kost2Reallocation.tooltip.ruleTarget"),
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
