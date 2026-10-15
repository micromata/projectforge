"use client";

import { GuardedLink } from "@/components/shared/guarded-link";
import { useTranslations } from "next-intl";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { useFormatContext } from "@/hooks/use-format";
import { formatDate, formatTimeRange } from "@/lib/format";
import type { IhkMissingDescription } from "./types";

/**
 * The week's time sheets without description, each with a link to its edit page: the report would get an
 * empty row for each of them. Saving the time sheet comes back here (`returnTo`, see the timesheet's
 * EditDef.returnTargets).
 */
export function MissingDescriptionList({
  items,
}: {
  items: IhkMissingDescription[];
}) {
  const t = useTranslations();
  const ctx = useFormatContext();
  return (
    <Alert>
      <AlertTitle className="text-destructive">
        {t("plugins.ihk.nodescriptionfound")}
      </AlertTitle>
      <AlertDescription className="flex flex-col gap-2">
        <p>{t("plugins.ihk.missing.hint")}</p>
        <ul className="flex flex-col divide-y">
          {items.map((item) => (
            <li
              key={item.id}
              className="flex flex-wrap items-center justify-between gap-2 py-1.5"
            >
              <span className="flex flex-col sm:flex-row sm:gap-3">
                <span className="tabular-nums">
                  {formatDate(item.startTime, ctx)}{" "}
                  {formatTimeRange(item.startTime, item.stopTime, ctx)}
                </span>
                <span className="text-muted-foreground">{item.label}</span>
              </span>
              <Button asChild size="sm" variant="outline">
                <GuardedLink
                  href={`/timesheet/${item.id}?returnTo=/ihk`}
                  aria-label={`${t("plugins.ihk.edit")}: ${formatDate(item.startTime, ctx)} ${item.label}`}
                >
                  {t("plugins.ihk.edit")}
                </GuardedLink>
              </Button>
            </li>
          ))}
        </ul>
      </AlertDescription>
    </Alert>
  );
}
