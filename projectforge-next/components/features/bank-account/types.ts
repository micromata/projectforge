/** A user or group as the DTO carries it: the id is what `copyTo` stores. */
// A type alias, not an interface: only an alias is assignable to the schema's loose object.
export type BankAccountRef = {
  id: number;
  displayName?: string;
};

/** org.projectforge.rest.dto.BankAccount. */
export interface BankAccountDetail {
  id: number | null;
  name?: string;
  bank?: string;
  iban?: string | null;
  bic?: string | null;
  description?: string | null;
  importSettings?: string | null;
  /** Read only: the help on the import settings (markdown), see BankAccountEntityRest. */
  importSettingsHelp?: string | null;
  fullAccessUsers?: BankAccountRef[];
  fullAccessGroups?: BankAccountRef[];
  readonlyAccessUsers?: BankAccountRef[];
  readonlyAccessGroups?: BankAccountRef[];
  minimalAccessUsers?: BankAccountRef[];
  minimalAccessGroups?: BankAccountRef[];
  created?: string | null;
  lastUpdate?: string | null;
}

/** Projection the list page renders — the same DTO, with the id the table keys rows by. */
export interface BankAccountListRow extends BankAccountDetail {
  id: number;
}
