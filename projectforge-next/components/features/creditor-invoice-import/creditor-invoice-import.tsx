"use client";

import { useMemo } from "react";
import { ImportFeature } from "@/components/shared/import/import-feature";
import type { ImportConfig } from "@/components/shared/import/import-types";
import { ENTITY } from "@/lib/rs/creditor-invoice-import";
import { CREDITOR_INVOICE_IMPORT_COLUMNS } from "./columns";

/**
 * The incoming-invoice (Kreditor) CSV/DATEV import. A thin consumer of the generic {@link ImportFeature}:
 * it only supplies the [ImportConfig] — the REST base (`incomingInvoiceImport`), the column layout and
 * where a commit returns to. Every screen, mutation and job handoff lives in the shared module, so the
 * address and banking imports will be the same handful of lines with their own config.
 */
export function CreditorInvoiceImport() {
  const config = useMemo<ImportConfig>(
    () => ({
      endpoints: { base: ENTITY },
      titleKey: "fibu.eingangsrechnung.import.title",
      columns: CREDITOR_INVOICE_IMPORT_COLUMNS,
      fileAccept: ".csv",
      returnRoute: "/creditor-invoice",
    }),
    []
  );

  return <ImportFeature config={config} />;
}
