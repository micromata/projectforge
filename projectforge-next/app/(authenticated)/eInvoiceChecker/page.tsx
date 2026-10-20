"use client";

import { EInvoiceCheckerPage } from "@/components/features/e-invoice-checker/e-invoice-checker-page";

/**
 * The e-invoice checker (`/next/eInvoiceChecker`), successor of the dynamic React page
 * `react/eInvoiceChecker/dynamic`. Takes no parameters, so no `<Suspense>`/`useSearchParams` is needed.
 */
export default function EInvoiceCheckerRoute() {
  return <EInvoiceCheckerPage />;
}
