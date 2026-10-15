"use client";

import { useStore } from "@tanstack/react-form";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { InputField } from "@/components/shared/form/input-field";
import { SelectField } from "@/components/shared/form/select-field";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { fetchSellerBankAccounts } from "@/lib/rs/account";
import type { AccountValues } from "./account-schema";

/**
 * The seller's bank account an invoice to this account is meant to name — a select over
 * `EInvoiceSellerConfig.bankAccounts`, as Wicket's `KontoEditForm` offers it, and clearable like its
 * `setNullValid(true)`. The value is the account's *name*, because that is what the column holds; the
 * label adds the IBAN.
 *
 * Where nothing is configured this falls back to a plain text box. Wicket leaves the field out
 * altogether there, but an account saved under an earlier configuration can still carry a name, and a
 * value that is stored has to remain visible and editable rather than silently disappear from the form.
 * A stored name no longer configured is kept as an extra option for the same reason.
 */
export function AccountSellerBankAccountField({
  className,
}: {
  className?: string;
}) {
  const t = useTranslations();
  const label = t("fibu.konto.sellerBankAccountName");
  const accounts = useQuery({
    queryKey: ["account", "sellerBankAccounts"],
    queryFn: ({ signal }) => fetchSellerBankAccounts(signal),
    // Application configuration: it changes about once per installation.
    staleTime: 60 * 60_000,
    refetchOnWindowFocus: false,
  }).data;
  const form = useEntityEditForm();
  const stored = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => (state.values as AccountValues).sellerBankAccountName
  );

  if (!accounts || accounts.length === 0) {
    return (
      <InputField
        name="sellerBankAccountName"
        label={label}
        className={className}
      />
    );
  }
  return (
    <SelectField
      name="sellerBankAccountName"
      label={label}
      options={
        stored && !accounts.some((a) => a.value === stored)
          ? [...accounts, { value: stored, label: stored }]
          : accounts
      }
      clearable
      className={className}
    />
  );
}
