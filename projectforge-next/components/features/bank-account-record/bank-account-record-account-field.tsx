"use client";

import { useTranslations } from "next-intl";
import { EntityAutocompleteField } from "@/components/shared/form/entity-autocomplete-field";
import { useEntityData } from "@/components/shared/form/form-context";
import { leafKeyOf } from "@/lib/leaf-key";
import type { BankAccountRecordDetail } from "./types";

/**
 * The account of a transaction: chosen for a new one, fixed for a stored one — an update doesn't move a
 * transaction to another account (the backend keeps the stored one, BankAccountRecordEntityRest.transformForDB).
 * Custom because the reference to BankAccountDO is no field of the record's metadata.
 */
export function BankAccountRecordAccountField({
  className,
}: {
  className?: string;
}) {
  const t = useTranslations();
  const stored = useEntityData<BankAccountRecordDetail>()?.id != null;
  return (
    <EntityAutocompleteField
      name="bankAccount"
      label={t(leafKeyOf("plugins.banking.account", t.has))}
      entity="bankAccount"
      metadataLess
      minChars={0}
      disabled={stored}
      className={className}
    />
  );
}
