"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { FileExportIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { resolveMenuUrl, toAbsoluteUrl } from "@/lib/menu-url";

/**
 * The export of the team vacation calendars (`VacationExportPageRest`), which the React list offered in its
 * menu. Not a page of this app yet, so the href goes through `resolveMenuUrl`: a full page load, and one line
 * to change when the page is migrated. It exports employees and groups of its own choosing, not the rows of
 * this list, so there is no filter to hand it.
 */
export function VacationListActions() {
  const t = useTranslations();
  const href = toAbsoluteUrl(resolveMenuUrl("react/vacationExport/dynamic"));
  return (
    <Button asChild type="button" variant="outline">
      <a href={href}>
        <HugeiconsIcon icon={FileExportIcon} aria-hidden />
        {t("vacation.export.title")}
      </a>
    </Button>
  );
}
