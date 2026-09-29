"use client";

import type { ReactNode } from "react";
import { useTranslations } from "next-intl";
import { CHART_ROLE } from "@/lib/charts/roles";

/**
 * One chart's legend sentence, e.g. "The last 45 days results in 320 workhours. …". The figures fill the
 * numbered placeholders (`{0}` → `arg0`, …) of the backend message and are already locale-formatted by the
 * caller. As on the former Wicket page, the two key figures are emphasised in the series' colours — the
 * message wraps them in `<red>` / `<green>` tags (red = "Soll"/actual latency, green = "Ist"/goal), rendered
 * here via `t.rich` in the same CSS tokens the chart uses.
 */
export function DisciplineLegend({
  messageKey,
  values,
}: {
  messageKey: string;
  values: Record<string, string | number>;
}) {
  const t = useTranslations();
  return (
    <p className="text-sm text-muted-foreground">
      {t.rich(messageKey, {
        ...values,
        red: (chunks: ReactNode) => (
          <span className="font-medium" style={{ color: CHART_ROLE.target }}>
            {chunks}
          </span>
        ),
        green: (chunks: ReactNode) => (
          <span className="font-medium" style={{ color: CHART_ROLE.actual }}>
            {chunks}
          </span>
        ),
      })}
    </p>
  );
}
