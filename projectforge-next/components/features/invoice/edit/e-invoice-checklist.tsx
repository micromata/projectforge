"use client";

import { useTranslations } from "next-intl";
import { FormAlert } from "@/components/shared/form-alert";
import { Spinner } from "@/components/shared/spinner";

/**
 * What stands between this invoice and an e-invoice of it — Wicket's error line above its two export
 * buttons (`RechnungEditForm.EInvoiceModalDialog`).
 *
 * A hint and not a gate: the fields the list names are on this very page, above it, and hiding anything
 * while one of them is empty would take away the place where it gets filled in. Only the two exports are
 * refused (see EInvoiceActions), because the backend refuses them too.
 *
 * The sentences arrive translated (`EInvoiceExportService.validate`) and are rendered as they come; an
 * unconfigured seller is one of them rather than a case of its own, so this needs no `configured` flag.
 */
export function EInvoiceChecklist({
  errors,
  isPending,
}: {
  errors: string[];
  /** While the answer is still on its way — the list decides what the buttons below may do. */
  isPending: boolean;
}) {
  const t = useTranslations();
  if (isPending) {
    return (
      <div className="flex py-2">
        <Spinner className="h-4 w-4 border-2" />
      </div>
    );
  }
  if (errors.length === 0) return null;
  return (
    <FormAlert tone="error">
      <p className="font-medium">
        {t("fibu.rechnung.eInvoice.validationErrors")}
      </p>
      <ul className="mt-1 list-disc pl-5">
        {errors.map((error) => (
          <li key={error}>{error}</li>
        ))}
      </ul>
    </FormAlert>
  );
}
