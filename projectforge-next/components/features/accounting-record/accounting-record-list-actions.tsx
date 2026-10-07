"use client";

import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { HugeiconsIcon } from "@hugeicons/react";
import { FileImportIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { ExcelExportButton } from "@/components/shared/excel-export-button";
import { downloadListExcel } from "@/lib/rs/list-export";
import type { MagicFilter } from "@/lib/rs/types";
import { navigateInGesture } from "@/lib/navigate-in-gesture";

/**
 * The toolbar of the accounting-record list: the Excel export of the filtered list, as the legacy Wicket
 * `AccountingRecordListPage` offered it ("exportAsXls"; `AccountingRecordEntityRest.exportAsExcel`), and
 * the way to the DATEV import (records and chart of accounts).
 *
 * No access check of its own: the list itself already requires the FIBU_DATEV_IMPORT right, which is the
 * export's and the import's right too (and is enforced by their endpoints).
 */
export function AccountingRecordListActions({
  filter,
}: {
  filter: MagicFilter;
}) {
  const t = useTranslations();
  const router = useRouter();
  return (
    <>
      <ExcelExportButton
        download={() => downloadListExcel("accountingRecord", filter)}
      />
      <Button
        type="button"
        variant="outline"
        onClick={() => navigateInGesture(router, "/datev-import")}
      >
        <HugeiconsIcon icon={FileImportIcon} />
        {t("import._")}
      </Button>
    </>
  );
}
