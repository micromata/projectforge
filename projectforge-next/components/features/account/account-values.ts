import type { AccountValues } from "./account-schema";
import type { KontoDetail } from "./types";

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`) arrives as `undefined`; every
 * value is normalised here, so no field ever holds `undefined` — which a controlled input would read as
 * "uncontrolled" and the schema as a missing value.
 */
export function toFormValues(konto: KontoDetail): AccountValues {
  return {
    nummer: konto.nummer ?? null,
    status: konto.status ?? null,
    bezeichnung: konto.bezeichnung ?? "",
    description: konto.description ?? null,
    contactPerson: konto.contactPerson ?? null,
    street: konto.street ?? null,
    zipCode: konto.zipCode ?? null,
    city: konto.city ?? null,
    country: konto.country ?? null,
    vatId: konto.vatId ?? null,
    leitwegId: konto.leitwegId ?? null,
    eInvoiceEmail: konto.eInvoiceEmail ?? null,
    sellerBankAccountName: konto.sellerBankAccountName ?? null,
  };
}

/**
 * Blank form for an account that doesn't exist yet. The status starts unset like Wicket's
 * (`KontoEditForm`: `setNullValid(true)`), and the number empty, so none is silently proposed.
 */
export function emptyAccountValues(): AccountValues {
  return toFormValues({ id: null });
}
