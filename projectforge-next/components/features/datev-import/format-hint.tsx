"use client";

import { useTranslations } from "next-intl";
import { SectionCard } from "@/components/shared/section-card";

/**
 * The file format of the DATEV import, shown at the foot of the page: the tax office's original file is read
 * unchanged — the month sheet as booking batch (later voucher dates included), the month-prefixed chart of
 * accounts, and the report sheets ignored. Mirrors DatevRecordExcelImporter / DatevAccountExcelImporter.
 */
export function DatevImportFormatHint() {
  const t = useTranslations();
  return (
    <SectionCard className="flex flex-col gap-2 bg-muted/40">
      <h3 className="text-sm font-semibold">
        {t("fibu.datev.import.format.title")}
      </h3>
      <p className="text-xs text-muted-foreground">
        {t("fibu.datev.import.format.intro")}
      </p>
      <p className="text-xs text-muted-foreground">
        {t("fibu.datev.import.format.records")}
      </p>
      <p className="text-xs text-muted-foreground">
        {t("fibu.datev.import.format.accounts")}
      </p>
    </SectionCard>
  );
}
