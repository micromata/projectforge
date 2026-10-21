/**
 * The routes of the banking pages that lead from one to the other: the transactions of an account and their import.
 * The parameter names are read by the routes themselves (`app/(authenticated)/bankAccountRecord`,
 * `app/(authenticated)/bankAccount/import`).
 */
export const BANK_ACCOUNT_ID_PARAM = "bankAccountId";

/** The transactions, of the given account only (a transient filter) or as the user left them. */
export function bankAccountRecordsHref(bankAccountId?: number | null): string {
  return bankAccountId != null
    ? `/bankAccountRecord?${BANK_ACCOUNT_ID_PARAM}=${bankAccountId}`
    : "/bankAccountRecord";
}

/** The import of transactions, into the given account (preselected) or one still to be chosen. */
export function bankAccountImportHref(bankAccountId?: number | null): string {
  return bankAccountId != null
    ? `/bankAccount/import?${BANK_ACCOUNT_ID_PARAM}=${bankAccountId}`
    : "/bankAccount/import";
}
