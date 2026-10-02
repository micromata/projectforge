"use client";

import { useTranslations } from "next-intl";
import { PageShell } from "@/components/shared/page-shell";
import { LegacyPageLink } from "@/components/shared/legacy-page-link";
import { DatevImport } from "@/components/features/datev-import/datev-import";
import { DatevImportFormatHint } from "@/components/features/datev-import/format-hint";
import { leafKeyOf } from "@/lib/leaf-key";

/**
 * The DATEV import (`/next/datev-import`, MenuItemDefId.DATEV_IMPORT): accounting records and chart of
 * accounts from the tax office's original xlsx. A concrete route rather than a list category: the import is no
 * REST list, and the flow lives in the shared import module (see components/shared/import).
 *
 * FIBU_DATEV_IMPORT only. Enforced by the endpoints behind it (`DatevRecordImportRest`,
 * `DatevAccountImportRest`); this page merely doesn't offer what would answer 403.
 */
export default function DatevImportPage() {
  const t = useTranslations();
  return (
    <PageShell>
      <div className="flex items-center gap-3 border-b bg-background px-4 py-3">
        <h1 className="text-lg font-bold tracking-tight">
          {t(leafKeyOf("fibu.datev.import", t.has))}
        </h1>
        <div className="flex-1" />
        {/* The way back to the Wicket import page, kept as legacy version. */}
        <LegacyPageLink url="wa/datevImport?legacyEscape" />
      </div>
      <div className="flex min-h-0 flex-1 flex-col gap-3 p-4">
        <DatevImport />
        <DatevImportFormatHint />
      </div>
    </PageShell>
  );
}
