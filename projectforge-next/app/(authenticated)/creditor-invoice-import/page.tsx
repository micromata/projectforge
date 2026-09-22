"use client";

import { useTranslations } from "next-intl";
import { PageShell } from "@/components/shared/page-shell";
import { LegacyPageLink } from "@/components/shared/legacy-page-link";
import { CreditorInvoiceImport } from "@/components/features/creditor-invoice-import/creditor-invoice-import";

/**
 * The incoming-invoice (Kreditor) CSV/DATEV import (`/next/creditor-invoice-import`), reached from the
 * creditor-invoice list's action bar. A concrete route rather than a list category: the import is no REST
 * list, and the flow lives in the shared import module (see components/shared/import).
 *
 * Finance only. Enforced by the endpoints behind it (`IncomingInvoiceImportRest`); this page and the
 * button that leads here merely don't offer what would answer 403.
 */
export default function CreditorInvoiceImportPage() {
  const t = useTranslations();

  return (
    <PageShell>
      <div className="flex items-center gap-3 border-b bg-background px-4 py-3">
        <h1 className="text-lg font-bold tracking-tight">
          {t("fibu.eingangsrechnung.import.title")}
        </h1>
        <div className="flex-1" />
        {/* The way back to the still-live Wicket/React upload page (uploadIncomingInvoices),
            the escape hatch until this hand-built import fully replaces it. */}
        <LegacyPageLink url="react/uploadIncomingInvoices/dynamic" />
      </div>
      <div className="flex min-h-0 flex-1 flex-col p-4">
        <CreditorInvoiceImport />
      </div>
    </PageShell>
  );
}
