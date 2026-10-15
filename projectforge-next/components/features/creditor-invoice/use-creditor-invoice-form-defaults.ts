"use client";

import { useQuery } from "@tanstack/react-query";
import {
  fetchCreditorInvoiceFormDefaults,
  type CreditorInvoiceFormDefaults,
} from "@/lib/rs/creditor-invoice";

/** The query key, exported so a test or a prefetch can address the same cache entry. */
export const creditorInvoiceFormDefaultsQueryKey = [
  "incomingInvoice",
  "formDefaults",
];

/**
 * The one configuration value the incoming invoice form needs before the user touches it: the default VAT
 * rate of a new position.
 *
 * Application configuration, so it changes when the installation is reconfigured and not while anyone is
 * editing an invoice — hence the long `staleTime` and no refetch on focus.
 *
 * `undefined` until the first answer arrives. Callers treat that as "no default", which is also the answer
 * for an installation that configured none: a field that starts empty is correct in both cases.
 */
export function useCreditorInvoiceFormDefaults():
  | CreditorInvoiceFormDefaults
  | undefined {
  const query = useQuery({
    queryKey: creditorInvoiceFormDefaultsQueryKey,
    queryFn: ({ signal }) => fetchCreditorInvoiceFormDefaults(signal),
    staleTime: 60 * 60_000,
    refetchOnWindowFocus: false,
  });
  return query.data;
}
