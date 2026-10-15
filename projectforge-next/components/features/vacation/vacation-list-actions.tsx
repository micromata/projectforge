"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { FileExportIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";

/**
 * The team vacation calendar (VacationExportPage), which the React list offered in its menu. It shows and
 * exports employees and groups of its own choosing, not the rows of this list, so there is no filter to hand it.
 */
export function VacationListActions() {
  const t = useTranslations();
  return (
    <Button asChild type="button">
      <Link href="/vacationExport">
        <HugeiconsIcon icon={FileExportIcon} aria-hidden />
        {t("vacation.export.title")}
      </Link>
    </Button>
  );
}
