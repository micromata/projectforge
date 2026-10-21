"use client";

import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  FileImportIcon,
  TransactionHistoryIcon,
} from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { navigateInGesture } from "@/lib/navigate-in-gesture";
import {
  bankAccountImportHref,
  bankAccountRecordsHref,
} from "./bank-account-links";

/** The two ways on from the list of bank accounts, as the React list offered them: the transactions and their import. */
export function BankAccountListActions() {
  const t = useTranslations();
  const router = useRouter();
  return (
    <>
      <Button
        type="button"
        variant="outline"
        onClick={() => navigateInGesture(router, bankAccountRecordsHref())}
      >
        <HugeiconsIcon icon={TransactionHistoryIcon} />
        {t("plugins.banking.account.record.title.list")}
      </Button>
      <Button
        type="button"
        variant="outline"
        onClick={() => navigateInGesture(router, bankAccountImportHref())}
      >
        <HugeiconsIcon icon={FileImportIcon} />
        {t("import._")}
      </Button>
    </>
  );
}
