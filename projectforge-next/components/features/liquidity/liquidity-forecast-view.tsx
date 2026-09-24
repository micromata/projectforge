"use client";

import { useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { DateInput } from "@/components/shared/date-input";
import { NumberBox } from "@/components/shared/form/number-box";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { useDebouncedValue } from "@/hooks/use-debounced-value";
import {
  fetchLiquidityForecast,
  fetchLiquidityForecastSettings,
  type LiquidityForecastRequest,
  type LiquidityForecastSettings,
} from "@/lib/rs/liquidity";
import { LiquidityForecastBalanceChart } from "./liquidity-forecast-balance-chart";
import { LiquidityForecastCashflowChart } from "./liquidity-forecast-cashflow-chart";

/** The forecast horizons offered — days from the base date on. */
const NEXT_DAYS_OPTIONS = [30, 60, 90, 180, 365] as const;
const DEFAULT_NEXT_DAYS = 90;

/**
 * The "Liquiditätsvorschau" tab of `/next/liquidity`, the successor of the Wicket `LiquidityForecastPage`.
 * Loads the user's last-used parameters (persisted as in Wicket) and, once they are in, renders the control
 * panel and charts seeded with them.
 */
export function LiquidityForecastView() {
  const t = useTranslations();
  const settings = useQuery({
    queryKey: ["liquidity", "forecast", "settings"],
    queryFn: ({ signal }) => fetchLiquidityForecastSettings(signal),
  });

  if (settings.isError) {
    return (
      <p className="p-4 text-sm text-destructive">
        {settings.error instanceof Error
          ? settings.error.message
          : String(settings.error)}
      </p>
    );
  }
  if (settings.isPending) {
    return <p className="p-4 text-sm text-muted-foreground">{t("loading")}</p>;
  }
  return <LiquidityForecastControls settings={settings.data} />;
}

/**
 * The control panel (start amount, base date, forecast horizon) over the two forecast charts, seeded with
 * the user's last-used [settings]. Every control drives the charts directly — after a short debounce (as
 * the list's search box does), the request is re-posted, the charts follow and the parameters are persisted
 * by the backend, so there is no "apply" button.
 */
function LiquidityForecastControls({
  settings,
}: {
  settings: LiquidityForecastSettings;
}) {
  const t = useTranslations();
  const tf = useTranslations("plugins.liquidityplanning.forecast");
  const [startAmount, setStartAmount] = useState<number | null>(
    settings.startAmount ?? 0
  );
  const [baseDate, setBaseDate] = useState<string | null>(
    settings.baseDate ?? null
  );
  const [nextDays, setNextDays] = useState(
    settings.nextDays ?? DEFAULT_NEXT_DAYS
  );

  // Memoized so its reference only changes when an input does — the debounce then resets its timer on a
  // change, not on every render, and fires once the user pauses (250 ms, the search box's delay).
  const params = useMemo<LiquidityForecastRequest>(
    () => ({
      startAmount: startAmount ?? 0,
      baseDate: baseDate || null,
      nextDays,
    }),
    [startAmount, baseDate, nextDays]
  );
  const debouncedParams = useDebouncedValue(params);

  const query = useQuery({
    queryKey: ["liquidity", "forecast", debouncedParams],
    queryFn: ({ signal }) => fetchLiquidityForecast(debouncedParams, signal),
    // Keep the current charts on screen while the next request runs, so a changed control updates them
    // in place instead of flashing back to the loading state.
    placeholderData: keepPreviousData,
  });

  const points = query.data?.points ?? [];

  return (
    <div className="space-y-6 p-4">
      <div className="flex flex-wrap items-end gap-4">
        <div className="grid gap-1.5">
          <Label htmlFor="startAmount">{tf("startAmount")}</Label>
          <NumberBox
            id="startAmount"
            value={startAmount}
            onChange={setStartAmount}
            fractionDigits={0}
            grouped
            align="right"
            className="w-40"
          />
        </div>
        <div className="grid gap-1.5">
          <HintTooltip text={tf("baseDate.tooltip")} openOnTap>
            <Label htmlFor="baseDate">{tf("baseDate._")}</Label>
          </HintTooltip>
          <DateInput
            id="baseDate"
            value={baseDate}
            onChange={setBaseDate}
            aria-label={tf("baseDate._")}
          />
        </div>
        <div className="grid gap-1.5">
          <Label htmlFor="nextDays">{tf("nextDays")}</Label>
          <Select
            value={String(nextDays)}
            onValueChange={(value) => setNextDays(Number(value))}
          >
            <SelectTrigger id="nextDays" className="w-32">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {NEXT_DAYS_OPTIONS.map((days) => (
                <SelectItem key={days} value={String(days)}>
                  {days}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      {query.isError ? (
        <p className="text-sm text-destructive">
          {query.error instanceof Error
            ? query.error.message
            : String(query.error)}
        </p>
      ) : query.isPending ? (
        <p className="text-sm text-muted-foreground">{t("loading")}</p>
      ) : points.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          {t("datatable.no-records-found")}
        </p>
      ) : (
        <div className="space-y-8">
          <section className="space-y-2">
            <h3 className="text-sm font-semibold">{tf("balance")}</h3>
            <LiquidityForecastBalanceChart data={points} />
          </section>
          <section className="space-y-2">
            <h3 className="text-sm font-semibold">{tf("cashflow")}</h3>
            <LiquidityForecastCashflowChart data={points} />
          </section>
        </div>
      )}
    </div>
  );
}
