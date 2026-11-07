"use client";

import { useTranslations } from "next-intl";
import { FormAlert } from "@/components/shared/form-alert";
import type { EInvoiceData } from "@/lib/rs/e-invoice-checker";

/**
 * The verdict on a checked e-invoice: valid, or the list of its errors, followed by the warnings.
 *
 * Shared by the checker page and the dialog that checks a freshly generated e-invoice.
 */
export function EInvoiceFindings({ data }: { data: EInvoiceData }) {
  const t = useTranslations();
  return (
    <div className="flex flex-col gap-2">
      {data.validationErrors.length === 0 ? (
        <FormAlert tone="success">{t("fibu.eInvoiceChecker.valid")}</FormAlert>
      ) : (
        <FormAlert tone="error">
          <p>{t("fibu.eInvoiceChecker.invalid")}</p>
          <ul className="mt-1 list-disc pl-5">
            {data.validationErrors.map((error, index) => (
              <li key={index}>{error}</li>
            ))}
          </ul>
        </FormAlert>
      )}
      {data.warnings.map((warning, index) => (
        <FormAlert key={index} tone="warning">
          {warning}
        </FormAlert>
      ))}
    </div>
  );
}
