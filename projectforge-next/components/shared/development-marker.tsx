"use client";

import type { CSSProperties } from "react";
import { useTranslations } from "next-intl";
import { Badge } from "@/components/ui/badge";
import { useSystemStatus } from "@/hooks/use-system-status";

/**
 * Says that this instance runs with `projectforge.development.mode=true`, or is a test system
 * (`projectforge.testsystemMode=true`), so that a page of such a system is never mistaken for the
 * productive one — they look alike down to the customer's logo, and the browser's url is no help behind
 * a proxy.
 *
 * The flags come from `SystemStatus`, which the backend also sends before a login
 * (SystemStatusRest.publicSystemData), so the marker is there on the login page as well. It rides the
 * query the logo row already has (staleTime: Infinity), hence no request of its own.
 *
 * The test-system badge takes the installation's colour (`projectforge.testsystemColor`), the one Wicket
 * painted its pages' background with; the backend only sends a plain CSS colour.
 */
export function DevelopmentMarker() {
  const t = useTranslations("system");
  const { data } = useSystemStatus();

  const testsystem = data?.testsystemMode === true;
  if (!data?.developmentMode && !testsystem) return null;
  return (
    // mx-auto centres it in what the two logos leave over, without touching their own alignment
    // (the row is justify-between and the wordmark carries ml-auto).
    <div className="mx-auto flex items-center gap-2">
      {testsystem && (
        <Badge
          // The state, readable from outside (see the development badge below).
          data-testsystem-mode="true"
          className="h-6 bg-testsystem px-3 text-xs font-semibold tracking-wide text-testsystem-foreground uppercase"
          style={
            data.testsystemColor
              ? ({ "--testsystem": data.testsystemColor } as CSSProperties)
              : undefined
          }
        >
          {t("testSystem")}
        </Badge>
      )}
      {data.developmentMode && (
        <Badge
          // The state, readable from outside, the way LogoRow publishes its collapse (see
          // e2e/logo-row.spec.ts): the text is a translation and no anchor for a test.
          data-development-mode="true"
          className="h-6 bg-development px-3 text-xs font-semibold tracking-wide text-development-foreground uppercase"
        >
          {t("developmentSystem")}
        </Badge>
      )}
    </div>
  );
}
