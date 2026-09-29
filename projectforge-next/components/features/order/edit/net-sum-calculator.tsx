"use client";

import { useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  Calculator01Icon,
  Delete02Icon,
  PlusSignIcon,
} from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { NumberBox } from "@/components/shared/form/number-box";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency, formatNumber } from "@/lib/format";

/** One line of the scratchpad — a role (free text), its daily rate and the person days spent on it. */
interface CalcRow {
  label: string;
  dayRate: number | null;
  personDays: number | null;
}

const emptyRow = (): CalcRow => ({
  label: "",
  dayRate: null,
  personDays: null,
});

/** A row carries data worth keeping the moment any of its three inputs is filled in. */
const isMeaningful = (row: CalcRow) =>
  row.label.trim() !== "" || row.dayRate != null || row.personDays != null;

/**
 * Parse the persisted breakdown JSON back into rows. Tolerant on purpose — a blob that predates a
 * field or was hand-edited must not throw, it falls back to a single empty line.
 */
const parseRows = (json: string | null | undefined): CalcRow[] => {
  if (!json) return [emptyRow()];
  try {
    const parsed = JSON.parse(json);
    if (!Array.isArray(parsed) || parsed.length === 0) return [emptyRow()];
    return parsed.map((row) => ({
      label: typeof row?.label === "string" ? row.label : "",
      dayRate: typeof row?.dayRate === "number" ? row.dayRate : null,
      personDays: typeof row?.personDays === "number" ? row.personDays : null,
    }));
  } catch {
    return [emptyRow()];
  }
};

export interface NetSumCalculatorProps {
  /** Full form-field name of the position's net sum, e.g. `positionen[2].nettoSumme`. */
  netSumName: string;
  /** Full form-field name of the position's person days, e.g. `positionen[2].personDays`. */
  personDaysName: string;
  /** Full form-field name of the position's stored breakdown JSON, e.g. `positionen[2].calculationData`. */
  calculationDataName: string;
  /** The currency behind the daily-rate boxes — the user's, never spelled out (see NumberBox). */
  currency?: string;
  disabled?: boolean;
  /**
   * Controlled open state, so the row's header can open the calculator from its title link. Falls back
   * to internal state when both are omitted (the icon button beside the net sum opens it on its own).
   */
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
}

/**
 * A fold-out pocket calculator beside a position's net sum: one line per role with a daily rate and
 * the person days, summed to Σ(rate × days). "Übernehmen" writes that sum into the position's net sum
 * and the total person days into its `personDays` field — the numbers the user would otherwise reach
 * for on a side calculator before typing the result into the form.
 *
 * The breakdown itself is persisted too, as JSON in the position's `calculationData` field, so the
 * calculation behind the net sum survives save and can be reopened and edited (the header shows a
 * calculator icon whenever it holds data). Applying an emptied calculator clears that field again.
 */
export function NetSumCalculator({
  netSumName,
  personDaysName,
  calculationDataName,
  currency,
  disabled,
  open: controlledOpen,
  onOpenChange,
}: NetSumCalculatorProps) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const format = useFormatContext();
  const [internalOpen, setInternalOpen] = useState(false);
  const open = controlledOpen ?? internalOpen;
  const setOpen = onOpenChange ?? setInternalOpen;
  // Seeded from the persisted breakdown. The row is unmounted while its position is collapsed, so this
  // initializer runs fresh on each expand and picks up whatever was last applied.
  const [rows, setRows] = useState<CalcRow[]>(() =>
    parseRows(form.getFieldValue(calculationDataName as never) as never)
  );

  const totals = useMemo(() => {
    let netSum = 0;
    let personDays = 0;
    for (const row of rows) {
      netSum += (row.dayRate ?? 0) * (row.personDays ?? 0);
      personDays += row.personDays ?? 0;
    }
    return { netSum, personDays };
  }, [rows]);

  const update = (index: number, patch: Partial<CalcRow>) =>
    setRows((prev) =>
      prev.map((row, i) => (i === index ? { ...row, ...patch } : row))
    );
  const addRow = () => setRows((prev) => [...prev, emptyRow()]);
  // The last line stays: an empty calculator is one blank line, not none.
  const removeRow = (index: number) =>
    setRows((prev) =>
      prev.length > 1 ? prev.filter((_, i) => i !== index) : prev
    );

  const apply = () => {
    // Rounded to the two decimals the net sum is stored with — a sum of rate × days can carry more.
    form.setFieldValue(
      netSumName as never,
      Number(totals.netSum.toFixed(2)) as never
    );
    form.setFieldValue(
      personDaysName as never,
      Number(totals.personDays.toFixed(2)) as never
    );
    // Persist the breakdown behind the sum — or clear it, so an emptied calculator drops the header
    // marker instead of leaving a stale "[]" behind.
    const meaningful = rows.filter(isMeaningful);
    form.setFieldValue(
      calculationDataName as never,
      (meaningful.length > 0 ? JSON.stringify(meaningful) : null) as never
    );
    setOpen(false);
  };

  return (
    // `modal`, for the same reason the recent-timesheets popover is: opened from inside the edit
    // dialog, a non-modal layer would sit outside its scroll lock.
    <Popover open={open} onOpenChange={setOpen} modal>
      <PopoverTrigger asChild>
        <Button
          type="button"
          variant="outline"
          size="icon"
          disabled={disabled}
          aria-label={t("order.calculator.title")}
        >
          <HugeiconsIcon icon={Calculator01Icon} size={14} aria-hidden />
        </Button>
      </PopoverTrigger>
      <PopoverContent align="end" className="w-[26rem]">
        <div className="text-sm font-medium">{t("order.calculator.title")}</div>
        <div className="flex flex-col gap-1.5">
          {rows.map((row, index) => (
            <div key={index} className="flex items-center gap-1.5">
              <Input
                value={row.label}
                onChange={(e) => update(index, { label: e.target.value })}
                placeholder={t("order.calculator.role")}
                aria-label={t("order.calculator.role")}
                className="h-7 w-16 shrink-0"
              />
              {/* Quantity × rate: the person days first, then the daily rate — read as "3 days at
                  1.200 €". NumberBox forwards its `className` to the inner input, not its wrapper, so
                  the column widths are set on wrappers around it, not on the boxes themselves. */}
              <div className="w-20 shrink-0">
                <NumberBox
                  value={row.personDays}
                  onChange={(next) => update(index, { personDays: next })}
                  fractionDigits={2}
                  grouped
                  aria-label={t("projectmanagement.personDays._")}
                  className="h-7"
                />
              </div>
              <span aria-hidden className="text-muted-foreground">
                ×
              </span>
              <div className="min-w-0 flex-1">
                <NumberBox
                  value={row.dayRate}
                  onChange={(next) => update(index, { dayRate: next })}
                  fractionDigits={2}
                  suffix={currency}
                  grouped
                  aria-label={t("order.calculator.dayRate")}
                  className="h-7"
                />
              </div>
              <Button
                type="button"
                variant="ghost"
                size="icon-xs"
                onClick={() => removeRow(index)}
                disabled={rows.length <= 1}
                aria-label={t("order.calculator.removeRow")}
              >
                <HugeiconsIcon icon={Delete02Icon} size={12} aria-hidden />
              </Button>
            </div>
          ))}
        </div>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={addRow}
          className="self-start"
        >
          <HugeiconsIcon icon={PlusSignIcon} size={14} aria-hidden />
          {t("order.calculator.addRow")}
        </Button>
        <div className="flex items-center justify-between border-t pt-2 text-sm">
          <span className="text-muted-foreground">
            {formatNumber(totals.personDays, format, 2)}{" "}
            {t("projectmanagement.personDays.short")}
          </span>
          <span className="font-medium">
            {formatCurrency(totals.netSum, format)}
          </span>
        </div>
        <Button type="button" size="sm" onClick={apply} className="self-end">
          {t("order.calculator.apply")}
        </Button>
      </PopoverContent>
    </Popover>
  );
}
