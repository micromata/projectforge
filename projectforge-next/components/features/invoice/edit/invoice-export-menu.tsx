"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { leafKeyOf } from "@/lib/leaf-key";
import { downloadInvoiceWord } from "@/lib/rs/invoice";
import { InvoiceExportButton } from "../invoice-export-button";
import { useInvoiceFormDefaults } from "../use-invoice-form-defaults";
import { EInvoiceDialog } from "./e-invoice-dialog";

/**
 * The exports of one invoice, as Wicket offers them in the content menu of its edit page
 * (`RechnungEditPage.addExportMenu`, `addEInvoiceMenu`): one entry per variant of the configured Word
 * template, plus the e-invoice where the installation configured a seller.
 *
 * The e-invoice entry opens [EInvoiceDialog] rather than downloading, because it is two exports (XRechnung
 * and ZUGFeRD) and neither is possible for every invoice. Its label is the plain "E-Invoice" and not
 * Wicket's `fibu.rechnung.eInvoice.saveAndOpen`: next does not save on the way, so a label promising a save
 * would be wrong about the one difference that matters here.
 *
 * Offered for a **stored** invoice only, which is Wicket's rule too (it omits the menu while `isNew()`) —
 * here it holds for a second reason: the document is built from the invoice in the database, not from the
 * form (see `OutgoingInvoiceEntityRest.exportInvoiceWord`). So while the form has unsaved changes the export
 * is of the last saved state, and for an invoice that was never saved there is nothing to export at all. The
 * button says so instead of disappearing: an entry that is simply absent reads as "this installation has no
 * export".
 *
 * Where the installation configured no custom template there is exactly one, unnamed variant
 * (`InvoiceService.getTemplateVariants` answers `[""]` then) — a plain button then, since a menu of one
 * entry is a click for nothing.
 */
export function InvoiceExportMenu({
  invoiceId,
}: {
  invoiceId?: number | null;
}) {
  const t = useTranslations();
  const defaults = useInvoiceFormDefaults();
  const variants = defaults?.templateVariants ?? [];
  const [eInvoiceOpen, setEInvoiceOpen] = useState(false);

  const download = useMutation({
    mutationFn: (variant: string) => downloadInvoiceWord(invoiceId!, variant),
    onError: (error: unknown) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });

  // Nothing to offer until the configuration has arrived — rendering a button that cannot know its
  // variants yet would either flicker from one entry to several or export the wrong template.
  if (variants.length === 0) return null;

  // The label is the parent of the two keys below, so it travels as the generator's leaf (see leafKeyOf).
  const label = t(leafKeyOf("fibu.rechnung.exportInvoice", t.has));
  // The whole explanation while it is refused: what the export would do is only half the answer then, and
  // that the document is the *stored* invoice is the part a user cannot guess.
  const tooltip =
    invoiceId == null ? t("fibu.rechnung.exportInvoice.onlyStored") : label;
  // The e-invoice, under the same condition Wicket adds its own menu for (`addEInvoiceMenu`): a seller that
  // is configured. Plus a stored invoice, since the export is of the stored state (see EInvoiceDialog).
  const eInvoice = invoiceId != null && defaults?.eInvoiceConfigured === true;

  // A single Word variant and no e-invoice, or nothing to export yet: a bare button either way — a menu
  // whose entries cannot be reached is a menu that shouldn't be a menu.
  if (invoiceId == null || (variants.length === 1 && !eInvoice)) {
    return (
      <InvoiceExportButton
        tooltip={tooltip}
        label={label}
        isPending={download.isPending}
        disabled={invoiceId == null}
        onClick={() => download.mutate(variants[0])}
      />
    );
  }

  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          {/* No `onClick`: the trigger's own opens the menu, and the entries are what downloads. */}
          <InvoiceExportButton
            tooltip={tooltip}
            label={label}
            isPending={download.isPending}
          />
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          {variants.map((variant) => (
            <DropdownMenuItem
              key={variant}
              onSelect={() => download.mutate(variant)}
            >
              {/* The only Word entry beside the e-invoice ones says what it exports rather than which
                  template it uses — "Default template" alone doesn't say that a document comes out. */}
              {variants.length === 1 ? label : variantLabel(variant, t)}
            </DropdownMenuItem>
          ))}
          {eInvoice && (
            <>
              <DropdownMenuSeparator />
              {/* Wicket's own label, minus the "save and": next doesn't save on the way (see
                  EInvoiceDialog) — but the two exports live behind one entry there as here. */}
              <DropdownMenuItem onSelect={() => setEInvoiceOpen(true)}>
                {t("fibu.konto.eInvoice")}
              </DropdownMenuItem>
            </>
          )}
        </DropdownMenuContent>
      </DropdownMenu>
      {/* Beside the menu, not inside it: the menu closes when the entry is chosen. */}
      {eInvoiceOpen && invoiceId != null && (
        <EInvoiceDialog
          invoiceId={invoiceId}
          onClose={() => setEInvoiceOpen(false)}
        />
      )}
    </>
  );
}

/**
 * What one entry is called, as Wicket names it: the variant with its underscores read as spaces, and
 * "default" for the unnamed one.
 *
 * The name is a file name fragment ("MMInvoiceTemplate_Commerzbank.docx" → "Commerzbank"), so it is the
 * installation's own word and not translatable — only the fallback is.
 */
function variantLabel(variant: string, t: (key: string) => string): string {
  return variant
    ? variant.replace(/_/g, " ")
    : t("fibu.rechnung.exportInvoice.template.default");
}
