"use client";

import { useTranslations } from "next-intl";
import { useMutation, useQuery } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import { Download04Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { EditPageTabs } from "@/components/shared/edit-page-tabs";
import { EntityEditHeader } from "@/components/shared/edit/entity-edit-header";
import { entityTabs } from "@/components/shared/edit/entity-tabs";
import { useEntityDetail } from "@/hooks/use-entity-detail";
import { useLegacyEditUrl } from "@/hooks/use-legacy-edit-url";
import {
  downloadOrderForecastJson,
  fetchOrderForecastAnalysis,
} from "@/lib/rs/order";
import { ORDER_PAGE, FORECAST_TAB_ID } from "../order.page";
import type { OrderDetail } from "../types";

/**
 * The forecast analysis of one order: what the backend expects to be invoiced per month.
 *
 * A page of its own rather than a section of the form, and read-only: the analysis is computed over the
 * **saved** order (`ForecastOrderAnalysis.htmlExport` does its own `find`, and its own access check), so
 * it would contradict a form still being edited.
 *
 * The table is the backend's HTML, inserted as it is — the only place in this app that does. It is the
 * export the finance department already knows, and rebuilding its layout here would be a second
 * forecast to keep in step with `ForecastExport`. The source is the same server that serves this page,
 * over an authenticated call.
 */
export function OrderForecastPage({ id }: { id: number }) {
  const t = useTranslations();
  const { data: order } = useEntityDetail<OrderDetail>(ORDER_PAGE.entity, id);
  const legacyUrl = useLegacyEditUrl(ORDER_PAGE.entity, id);

  const analysis = useQuery({
    queryKey: [ORDER_PAGE.entity, id, "forecastAnalysis"],
    queryFn: ({ signal }) => fetchOrderForecastAnalysis(id, signal),
  });
  // A download, not a query: it writes a file and has nothing to cache.
  const download = useMutation({
    mutationFn: () => downloadOrderForecastJson(id),
  });

  const tabs = entityTabs({
    sections: ORDER_PAGE.edit.sections,
    t,
    id,
    route: ORDER_PAGE.route,
    history: ORDER_PAGE.metadata.historizable,
    extraTabs: ORDER_PAGE.edit.extraTabs,
    onFormPage: false,
  });

  return (
    <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
      <div className="shrink-0">
        <EntityEditHeader
          listRoute={ORDER_PAGE.route}
          listLabel={t(ORDER_PAGE.titleKey)}
          title={order ? ORDER_PAGE.edit.title(order) : ""}
          legacyUrl={legacyUrl}
        />
      </div>
      <EditPageTabs tabs={tabs} activeId={FORECAST_TAB_ID} />
      <div className="flex-1 overflow-y-auto bg-muted/30 px-6 pt-4 pb-6">
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
          {/* The JSON export exists in development mode only and answers 404 otherwise, which cannot
              be known beforehand — so the failure is reported rather than the button hidden. */}
          {download.isError && (
            <span className="text-sm text-destructive">
              {t("order.forecast.jsonUnavailable")}
            </span>
          )}
        </div>
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
      </div>
    </div>
  );
}
