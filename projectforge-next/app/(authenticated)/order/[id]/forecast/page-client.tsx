"use client";

import { EntityTabRedirect } from "@/components/shared/edit/entity-tab-redirect";
import { FORECAST_TAB_ID } from "@/components/features/order/order.page";

// The forecast used to be a page of its own; it is a tab of the edit page now (see EntityTabRedirect).
export function OrderForecastPageClient() {
  return (
    <EntityTabRedirect
      pattern="/order/[id]/forecast"
      route="/order"
      tab={FORECAST_TAB_ID}
    />
  );
}
