"use client";

import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { toast } from "@/lib/toast";
import { leafKeyOf } from "@/lib/leaf-key";
import { ExportButton } from "@/components/shared/export-button";
import {
  downloadCreditorInvoiceTransfer,
  downloadCreditorInvoiceTransfers,
} from "@/lib/rs/creditor-invoice";

/**
 * The "Export bank transfers" button of the incoming invoice, as Wicket's `EingangsrechnungEditPage`
 * offers it for one invoice and its multi-select page for a selection: a SEPA pain.001.003.03 xml download.
 *
 * Two callers, one button. In the edit form it exports the single invoice by id (`selection` absent); on the
 * mass-update page it exports the session selection (`selection`), which needs no id because the picked ids
 * live in the HTTP session (see lib/rs/multi-select.ts).
 *
 * Offered for a **stored** invoice only, Wicket's rule too: the xml is built from the invoice in the
 * database, not from the form, so an unsaved invoice has nothing to export. The button then says why rather
 * than vanishing — an absent button reads as "this installation has no export".
 *
 * A validation failure (missing IBAN/BIC/…, a foreign-currency invoice, an empty selection) comes back as a
 * downloaded `error.txt`, matching the legacy export; a transport error is toasted.
 */
export function CreditorInvoiceTransferButton(
  props: { invoiceId?: number | null } | { selection: true }
) {
  const t = useTranslations();
  const isSelection = "selection" in props;
  const invoiceId = isSelection ? null : props.invoiceId;

  const download = useMutation({
    mutationFn: () =>
      isSelection
        ? downloadCreditorInvoiceTransfers()
        : downloadCreditorInvoiceTransfer(invoiceId!),
    onError: (error: unknown) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });

  // The label is the parent of the .tooltip/.error keys below, so it travels as the generator's leaf.
  const label = t(leafKeyOf("fibu.rechnung.transferExport", t.has));
  const disabled = !isSelection && invoiceId == null;
  const tooltip = disabled
    ? t("fibu.rechnung.exportInvoice.onlyStored")
    : t("fibu.rechnung.transferExport.tooltip");

  return (
    <ExportButton
      tooltip={tooltip}
      label={label}
      isPending={download.isPending}
      disabled={disabled}
      onClick={() => download.mutate()}
    />
  );
}
