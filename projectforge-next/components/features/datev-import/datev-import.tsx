"use client";

import { useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import { ImportFeature } from "@/components/shared/import/import-feature";
import type { ImportConfig } from "@/components/shared/import/import-types";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { AccountingRecordBwa } from "@/components/features/accounting-record/accounting-record-bwa";
import type { BwaStatistics } from "@/components/features/accounting-record/types";
import { ACCOUNT_ENTITY, RECORD_ENTITY } from "@/lib/rs/datev-import";
import { DATEV_ACCOUNT_IMPORT_COLUMNS } from "./account-columns";
import { DATEV_RECORD_IMPORT_COLUMNS } from "./record-columns";

type Tab = "records" | "accounts";

/**
 * The DATEV import: two tabs, each a consumer of the generic {@link ImportFeature} with its own upload,
 * preview and commit, and both accepting the same original file of the tax office. The records tab shows the
 * BWA of the importable records above the preview (`meta.bwa`, see DatevRecordImportRest), so it can be checked
 * against the tax office's BWA sheet before committing.
 */
export function DatevImport() {
  const t = useTranslations();
  const [tab, setTab] = useState<Tab>("records");

  const recordConfig = useMemo<ImportConfig>(
    () => ({
      endpoints: { base: RECORD_ENTITY },
      titleKey: "fibu.datev.import.records",
      columns: DATEV_RECORD_IMPORT_COLUMNS,
      fileAccept: ".xlsx,.xls",
      returnRoute: "/accounting-record",
      stayAfterCommit: true,
      renderAboveTable: (view) => (
        <AccountingRecordBwa
          statistics={view.meta?.bwa as BwaStatistics | undefined}
        />
      ),
    }),
    []
  );
  const accountConfig = useMemo<ImportConfig>(
    () => ({
      endpoints: { base: ACCOUNT_ENTITY },
      titleKey: "fibu.datev.import.accounts",
      columns: DATEV_ACCOUNT_IMPORT_COLUMNS,
      fileAccept: ".xlsx,.xls",
      returnRoute: "/account",
      stayAfterCommit: true,
    }),
    []
  );

  return (
    <Tabs
      value={tab}
      onValueChange={(value) => setTab(value as Tab)}
      className="flex flex-col gap-3"
    >
      <TabsList className="w-fit">
        <TabsTrigger value="records">
          {t("fibu.datev.import.records")}
        </TabsTrigger>
        <TabsTrigger value="accounts">
          {t("fibu.datev.import.accounts")}
        </TabsTrigger>
      </TabsList>
      <TabsContent value="records">
        <ImportFeature config={recordConfig} />
      </TabsContent>
      <TabsContent value="accounts">
        <ImportFeature config={accountConfig} />
      </TabsContent>
    </Tabs>
  );
}
