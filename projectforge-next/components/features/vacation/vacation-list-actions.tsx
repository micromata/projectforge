"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { FileExportIcon, UserStatusIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";

/**
 * The availability entries (AVAILABILITY_PAGE), the other absences of the employees, and the team vacation calendar
 * (VacationExportPage), which the React list offered in its menu. It shows and exports employees and groups of its
 * own choosing, not the rows of this list, so there is no filter to hand it.
 */
export function VacationListActions() {
  const t = useTranslations();
  return (
    <>
      <Button asChild type="button" variant="ghost">
        <Link href="/availability">
          <HugeiconsIcon icon={UserStatusIcon} aria-hidden />
          {t("menu.availability")}
        </Link>
      </Button>
      <Button asChild type="button">
        <Link href="/vacationExport">
          <HugeiconsIcon icon={FileExportIcon} aria-hidden />
          {t("vacation.export.title")}
        </Link>
      </Button>
    </>
  );
}
