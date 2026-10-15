"use client";

import { useTranslations } from "next-intl";
import { InputField } from "@/components/shared/form/input-field";
import { SelectField } from "@/components/shared/form/select-field";
import { useInvoiceFormDefaults } from "../use-invoice-form-defaults";

/**
 * Which of the seller's bank accounts the invoice asks to be paid into — the account the e-invoice export
 * writes into the payment terms.
 *
 * A select over `EInvoiceSellerConfig.bankAccounts`, as Wicket offers it, and clearable like its
 * `setNullValid(true)`: an invoice may well name none. The value stays the IBAN and not an id, because that
 * is what the column holds and what `EInvoiceSellerConfig.findBankAccount` looks an account up by — the
 * accounts come from the application configuration and have no ids.
 *
 * Where nothing is configured this falls back to a plain text box. Wicket leaves the dropdown out
 * altogether there, but an invoice written under an earlier configuration can still carry an IBAN, and a
 * value that is stored has to remain visible and editable rather than silently disappear from the form.
 */
export function SellerBankAccountField({ className }: { className?: string }) {
  const t = useTranslations();
  const label = t("fibu.rechnung.sellerBankAccount");
  const accounts = useInvoiceFormDefaults()?.bankAccounts;

  if (!accounts || accounts.length === 0) {
    return (
      <InputField
        name="sellerBankAccount"
        label={label}
        className={className}
      />
    );
  }
  return (
    <SelectField
      name="sellerBankAccount"
      label={label}
      options={accounts}
      clearable
      className={className}
    />
  );
}
