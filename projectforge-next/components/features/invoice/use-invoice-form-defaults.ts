"use client";

import { useQuery } from "@tanstack/react-query";
import {
  fetchInvoiceFormDefaults,
  type InvoiceFormDefaults,
} from "@/lib/rs/invoice";

/** The query key, exported so a test or a prefetch can address the same cache entry. */
export const invoiceFormDefaultsQueryKey = ["outgoingInvoice", "formDefaults"];

/**
 * The four configuration values the invoice form needs before the user touches it: the default VAT rate of
 * a new position, the seller's bank accounts, whether an e-invoice export is configured, and the variants
 * of the Word template.
 *
 * One request instead of four, and one that is practically never repeated: all of it is application
 * configuration, so it changes when the installation is reconfigured and not while anyone is editing an
 * invoice. Hence the long `staleTime` and no refetch on focus — a stale VAT default is a preset the user
 * can overwrite, while a request per mount would be four round trips for a value that hasn't moved.
 *
 * `undefined` until the first answer arrives. Callers treat that as "no default", which is also the answer
 * for an installation that configured none: a field that starts empty is correct in both cases, while
 * blocking the form on this read would be a spinner in front of an empty invoice.
 */
export function useInvoiceFormDefaults(): InvoiceFormDefaults | undefined {
  const query = useQuery({
    queryKey: invoiceFormDefaultsQueryKey,
    queryFn: ({ signal }) => fetchInvoiceFormDefaults(signal),
    staleTime: 60 * 60_000,
    refetchOnWindowFocus: false,
  });
  return query.data;
}
