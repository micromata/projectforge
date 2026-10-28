"use client";

import { useMemo } from "react";
import { ImportFeature } from "@/components/shared/import/import-feature";
import type { ImportConfig } from "@/components/shared/import/import-types";
import { ENTITY } from "@/lib/rs/creditor-invoice-import";
import {
  CREDITOR_INVOICE_IMPORT_COLUMNS,
  CREDITOR_INVOICE_IMPORT_DIFF_ONLY_FIELDS,
} from "./columns";

/**
 * The incoming-invoice (Kreditor) CSV/DATEV import. A thin consumer of the generic {@link ImportFeature}:
 * it only supplies the [ImportConfig] — the REST base (`incomingInvoiceImport`) and the column layout. A commit
 * stays on the page, so the next file can be reconciled right away. Every screen, mutation and job handoff lives in the shared module, so the
 * address and banking imports will be the same handful of lines with their own config.
 */
export function CreditorInvoiceImport() {
  const config = useMemo<ImportConfig>(
    () => ({
      endpoints: { base: ENTITY },
      titleKey: "fibu.eingangsrechnung.import.title",
      descriptionKey: "fibu.eingangsrechnung.import.description",
      helpKey: "fibu.eingangsrechnung.import.templateInfo",
      columns: CREDITOR_INVOICE_IMPORT_COLUMNS,
      diffOnlyFields: CREDITOR_INVOICE_IMPORT_DIFF_ONLY_FIELDS,
      fileAccept: ".csv",
      returnRoute: "/creditor-invoice",
      stayAfterCommit: true,
    }),
    []
  );

  return <ImportFeature config={config} />;
}
