"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { BeachIcon, FileExportIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";

/**
 * The own vacation account (VacationAccountPage) and the team vacation calendar (VacationExportPage), with the
 * availability entries shown. It shows and exports users and groups of its own choosing, not the rows of this list,
 * so there is no filter to hand it.
 */
export function AvailabilityListActions() {
  const t = useTranslations();
  return (
    <>
      <Button asChild type="button" variant="ghost">
        <Link href="/vacationAccount">
          <HugeiconsIcon icon={BeachIcon} aria-hidden />
          {t("menu.vacation.leaveaccount")}
        </Link>
      </Button>
      <Button asChild type="button">
        <Link href="/vacationExport?showAvailabilities=true">
          <HugeiconsIcon icon={FileExportIcon} aria-hidden />
          {t("vacation.export.title")}
        </Link>
      </Button>
    </>
  );
}
