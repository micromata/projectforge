import type { BankAccountValues } from "./bank-account-schema";
import type { BankAccountDetail } from "./types";

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`) arrives as `undefined`; every value is
 * normalised here, so no field ever holds `undefined`.
 */
export function toFormValues(account: BankAccountDetail): BankAccountValues {
  return {
    id: account.id ?? null,
    name: account.name ?? "",
    bank: account.bank ?? "",
    iban: account.iban ?? null,
    bic: account.bic ?? null,
    description: account.description ?? null,
    importSettings: account.importSettings ?? null,
    fullAccessUsers: account.fullAccessUsers ?? [],
    fullAccessGroups: account.fullAccessGroups ?? [],
    readonlyAccessUsers: account.readonlyAccessUsers ?? [],
    readonlyAccessGroups: account.readonlyAccessGroups ?? [],
    minimalAccessUsers: account.minimalAccessUsers ?? [],
    minimalAccessGroups: account.minimalAccessGroups ?? [],
  };
}

/**
 * Blank form for an account that doesn't exist yet. The backend presets the logged-in user with full access
 * (`BankAccountEntityRest.newBaseDTO`), and its `/rs/bankAccount/newEntry` answer is what the form is reset onto.
 */
export function emptyBankAccountValues(): BankAccountValues {
  return toFormValues({ id: null });
}
