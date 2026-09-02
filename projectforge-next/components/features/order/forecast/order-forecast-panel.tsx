"use client";

import { useTranslations } from "next-intl";
import { useMutation, useQuery } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import { Download04Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  downloadOrderForecastJson,
  fetchOrderForecastAnalysis,
} from "@/lib/rs/order";
import { useSystemStatus } from "@/hooks/use-system-status";
// The bare constant, not ORDER_PAGE: the declaration names this component as its forecast tab, so
// reading a property off it here closes a cycle — and one whose type TypeScript then cannot infer
// (TS7022/TS7023, "referenced directly or indirectly in its own initializer"). The entity is a string
// and needs none of the declaration.
import { ORDER_ENTITY } from "../order.page";

/**
 * The forecast analysis of one order: what the backend expects to be invoiced per month.
 *
 * A tab beside the form and read-only: the analysis is computed over the **saved** order
 * (`ForecastOrderAnalysis.htmlExport` does its own `find`, and its own access check), so it would
 * contradict a form still being edited. Rendered only while its tab is open (see EditPageShell), so
 * the analysis is requested when it is looked at.
 *
 * The table is the backend's HTML, inserted as it is — the only place in this app that does. It is the
 * export the finance department already knows, and rebuilding its layout here would be a second
 * forecast to keep in step with `ForecastExport`. The source is the same server that serves this page,
 * over an authenticated call.
 */
export function OrderForecastPanel({ id }: { id: number }) {
  const t = useTranslations();

  // The JSON export is gated by `SystemStatus.isDevelopmentMode()` on the backend; the same flag rides
  // the system-status query the logo row already holds, so the button can be hidden up front rather
  // than only reporting the 404 after a click.
  const { data: systemStatus } = useSystemStatus();

  const analysis = useQuery({
    queryKey: [ORDER_ENTITY, id, "forecastAnalysis"],
    queryFn: ({ signal }) => fetchOrderForecastAnalysis(id, signal),
  });
  // A download, not a query: it writes a file and has nothing to cache.
  const download = useMutation({
    mutationFn: () => downloadOrderForecastJson(id),
  });

  return (
    <>
      {/* The analysis is computed over the saved order, see above — so say so where it is read. */}
      <p className="mb-3 text-sm text-muted-foreground">
        {t("order.forecast.savedOnlyHint")}
      </p>
      {/* The JSON export exists in development mode only (see above), so the button is shown there
          alone rather than answering a 404 on a productive system. */}
      {systemStatus?.developmentMode && (
        <div className="mb-3 flex items-center gap-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => download.mutate()}
            disabled={download.isPending}
          >
            <HugeiconsIcon icon={Download04Icon} size={14} aria-hidden />
            JSON
          </Button>
          {download.isError && (
            <span className="text-sm text-destructive">
              {t("order.forecast.jsonUnavailable")}
            </span>
          )}
        </div>
      )}
      {analysis.isPending && (
        <p className="text-sm text-muted-foreground">{t("loading")}</p>
      )}
      {analysis.isError && (
        <p className="text-sm text-destructive">
          {t("order.forecast.unavailable")}
        </p>
      )}
      {analysis.data && (
        <div
          className="order-forecast overflow-x-auto rounded-md border border-border bg-background p-4"
          // The server's own HTML export, see above.
          dangerouslySetInnerHTML={{ __html: analysis.data }}
        />
      )}
    </>
  );
}
