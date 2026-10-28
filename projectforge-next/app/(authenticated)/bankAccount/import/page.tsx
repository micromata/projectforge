"use client";

import { Suspense } from "react";
import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { PageShell } from "@/components/shared/page-shell";
import { useDocumentTitle } from "@/hooks/use-document-title";
import { BankAccountImport } from "@/components/features/bank-account-import/bank-account-import";
import { BANK_ACCOUNT_ID_PARAM } from "@/components/features/bank-account/bank-account-links";

/**
 * The import of bank account transactions (`/next/bankAccount/import`), reached from the list and the form of the
 * bank accounts. `?bankAccountId=` preselects the account.
 */
export default function BankAccountImportPage() {
  const t = useTranslations();
  const title = t("plugins.banking.account.record.import.title");
  useDocumentTitle(title);

  return (
    <PageShell>
      <div className="flex items-center gap-3 border-b bg-background px-4 py-3">
        <h1 className="text-lg font-bold tracking-tight">{title}</h1>
      </div>
      <div className="flex min-h-0 flex-1 flex-col p-4">
        {/* `useSearchParams` needs this boundary under `output: "export"`. */}
        <Suspense>
          <BankAccountImportWithParams />
        </Suspense>
      </div>
    </PageShell>
  );
}

function BankAccountImportWithParams() {
  const raw = useSearchParams().get(BANK_ACCOUNT_ID_PARAM);
  const id = raw != null ? Number(raw) : NaN;
  return (
    <BankAccountImport
      initialBankAccountId={Number.isInteger(id) && id > 0 ? id : null}
    />
  );
}
