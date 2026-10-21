/**
 * The import of the transactions of a bank account (`BankAccountRecordImportRest`, mapped to
 * `bankAccountRecordImport`). The upload, state, reconcile, commit and cancel calls are the generic ones of
 * ./import.ts bound to [ENTITY]; the account travels with the upload (field `bankAccountId`).
 */

/** REST path base of the import of bank account transactions — `BankAccountRecordImportRest` is mapped here. */
export const ENTITY = "bankAccountRecordImport";
