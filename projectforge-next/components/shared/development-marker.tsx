"use client";

import { useTranslations } from "next-intl";
import { Badge } from "@/components/ui/badge";
import { useSystemStatus } from "@/hooks/use-system-status";

/**
 * Says that this instance runs with `projectforge.development.mode=true`, so that a page of a
 * development system is never mistaken for the productive one — the two look alike down to the
 * customer's logo, and the browser's url is no help behind a proxy.
 *
 * The flag comes from `SystemStatus.developmentMode`, which the backend also sends before a login
 * (SystemStatusRest.publicSystemData), so the marker is there on the login page as well. It rides the
 * query the logo row already has (staleTime: Infinity), hence no request of its own.
 */
export function DevelopmentMarker() {
  const t = useTranslations("system");
  const { data } = useSystemStatus();

  if (!data?.developmentMode) return null;
  return (
    <Badge
      // The state, readable from outside, the way LogoRow publishes its collapse (see
      // e2e/logo-row.spec.ts): the text is a translation and no anchor for a test.
      data-development-mode="true"
      // mx-auto centres it in what the two logos leave over, without touching their own alignment
      // (the row is justify-between and the wordmark carries ml-auto).
      className="mx-auto h-6 bg-development px-3 text-xs font-semibold tracking-wide text-development-foreground uppercase"
    >
      {t("developmentSystem")}
    </Badge>
  );
}
