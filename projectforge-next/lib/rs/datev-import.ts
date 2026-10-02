/**
 * The two REST bases of the DATEV import, both fed by the same original file of the tax office: the accounting
 * records (`DatevRecordImportRest`) and the chart of accounts (`DatevAccountImportRest`). The generic import
 * client (./import.ts) is addressed by these bases directly.
 */

/** REST path base of the accounting record import. */
export const RECORD_ENTITY = "datevRecordImport";

/** REST path base of the chart of accounts import. */
export const ACCOUNT_ENTITY = "datevAccountImport";
