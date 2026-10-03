"use client";

import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { HugeiconsIcon } from "@hugeicons/react";
import { FileImportIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";

/**
 * The way to the DATEV import (records and chart of accounts) from the accounting-record list. No access
 * check of its own: the list itself already requires the FIBU_DATEV_IMPORT right, which is the import's
 * right too (and is enforced by its endpoints).
 */
export function AccountingRecordListActions() {
  const t = useTranslations();
  const router = useRouter();
  return (
    <Button
      type="button"
      variant="outline"
      onClick={() => router.push("/datev-import")}
    >
      <HugeiconsIcon icon={FileImportIcon} />
      {t("import._")}
    </Button>
  );
}
