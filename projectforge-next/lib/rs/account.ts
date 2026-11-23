/**
 * Rest calls specific to the account page (org.projectforge.rest.fibu.KontoEntityRest).
 *
 * The list and edit go through the generic entity client (`fetchList`/`fetchOne`/`save`); the only
 * thing special here is the choice of the seller's bank accounts for the e-invoice block.
 */

import { request } from "./client";
import { downloadFile } from "./download";

/** One entry of `KontoEntityRest.getSellerBankAccounts`: the stored name and a label with the IBAN. */
export interface SellerBankAccount {
  value: string;
  label: string;
}

export function fetchSellerBankAccounts(
  signal?: AbortSignal
): Promise<SellerBankAccount[]> {
  return request<SellerBankAccount[]>(
    "/rs/account/sellerBankAccounts",
    { method: "GET" },
    signal
  );
}

/**
 * The customer data sheet of an account as PDF (`KontoEntityRest.getCustomerDataSheet`), saved under the name
 * the backend gives it. A GET, because it only reads.
 */
export function downloadAccountDataSheet(
  id: number,
  signal?: AbortSignal
): Promise<void> {
  return downloadFile(
    `/rs/account/customerDataSheet/${id}`,
    { method: "GET" },
    signal
  );
}
