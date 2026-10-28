"use client";

import { useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { ImportFeature } from "@/components/shared/import/import-feature";
import type {
  ImportConfig,
  ImportView,
} from "@/components/shared/import/import-types";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
import { leafKeyOf } from "@/lib/leaf-key";
import { fetchOne } from "@/lib/rs/client";
import { ENTITY } from "@/lib/rs/bank-account-import";
import { BankAccountTargetSelect } from "./bank-account-target-select";
import { BANK_ACCOUNT_IMPORT_COLUMNS } from "./columns";

interface Props {
  /** The account preselected by the caller (`?bankAccountId=`), e.g. the form of the account. */
  initialBankAccountId: number | null;
}

/**
 * The import of the transactions of a bank account, a consumer of the generic {@link ImportFeature}. What it adds
 * is the account: chosen before the drop, it is sent with the upload, whose file is read with the account's import
 * settings — so it can't be changed afterwards, only shown. The user stays on the page after the commit; the
 * transactions are a click away.
 */
export function BankAccountImport({ initialBankAccountId }: Props) {
  const [picked, setPicked] = useState<EntityRef | null>(null);
  // The preselected account, read for its display name until the user picks another one.
  const initial = useQuery({
    queryKey: ["bankAccount", "importTarget", initialBankAccountId],
    queryFn: ({ signal }) =>
      fetchOne<{ id: number; name?: string; iban?: string }>(
        "bankAccount",
        initialBankAccountId!,
        signal
      ),
    enabled: initialBankAccountId != null && picked == null,
  });
  const initialAccount = initial.data;
  const account = useMemo<EntityRef | null>(
    () =>
      picked ??
      (initialAccount
        ? {
            id: initialAccount.id,
            displayName: initialAccount.name ?? initialAccount.iban ?? "",
          }
        : null),
    [picked, initialAccount]
  );

  const config = useMemo<ImportConfig>(
    () => ({
      endpoints: { base: ENTITY },
      titleKey: "plugins.banking.account.record.import.title",
      descriptionKey: "plugins.banking.account.record.import.description",
      columns: BANK_ACCOUNT_IMPORT_COLUMNS,
      fileAccept: ".csv",
      returnRoute: "/bankAccountRecord",
      stayAfterCommit: true,
      uploadFields:
        account != null ? { bankAccountId: String(account.id) } : undefined,
      renderBeforeDrop: () => (
        <BankAccountTargetSelect value={account} onChange={setPicked} />
      ),
      renderAboveTable: (view: ImportView) => <ImportTarget view={view} />,
    }),
    [account]
  );

  return <ImportFeature config={config} />;
}

/** The account the stashed file is imported into (`extraViewMeta`). */
function ImportTarget({ view }: { view: ImportView }) {
  const t = useTranslations();
  const name = view.meta?.bankAccountName;
  if (typeof name !== "string" || !name) return null;
  return (
    <p className="text-sm">
      <span className="text-muted-foreground">
        {t(leafKeyOf("plugins.banking.account", t.has))}:{" "}
      </span>
      <span className="font-semibold">{name}</span>
    </p>
  );
}
