/** The account of a transaction as the DTO carries it (`BankAccount.copyFromMinimal` plus name, IBAN and BIC). */
export interface BankAccountRecordAccount {
  id: number;
  name?: string | null;
  iban?: string | null;
  bic?: string | null;
}

/** org.projectforge.plugins.banking.BankAccountRecord. */
export interface BankAccountRecordDetail {
  id: number | null;
  bankAccount?: BankAccountRecordAccount | null;
  /** Read only: the stored checksum doesn't match the record (modified after its import). */
  checksumError?: boolean | null;
  amount?: number | null;
  date?: string | null;
  valueDate?: string | null;
  type?: string | null;
  subject?: string | null;
  comment?: string | null;
  currency?: string | null;
  debteeId?: string | null;
  mandateReference?: string | null;
  customerReference?: string | null;
  collectionReference?: string | null;
  info?: string | null;
  receiverSender?: string | null;
  iban?: string | null;
  bic?: string | null;
  created?: string | null;
  lastUpdate?: string | null;
}

/** Projection the list page renders — the same DTO, with the id the table keys rows by. */
export interface BankAccountRecordListRow extends BankAccountRecordDetail {
  id: number;
}

/** `BankAccountRecordEntityRest.Statistics`: the sums of the result set. */
export interface BankAccountRecordStatistics {
  income: number;
  outgo: number;
  cashflow: number;
}
