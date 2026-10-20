/**
 * The JSON value of the configuration parameter `fibu.accounting`, mirroring `AccountingConfig`,
 * `BusinessAssessmentConfig` and `BusinessAssessmentRowConfig` (projectforge-business): the account number
 * ranges of debitors and creditors and the rows of the business assessment (BWA). The backend validates it
 * on saving.
 */
export const ACCOUNTING_PARAM = "fibu.accounting";

/** `Priority` of projectforge-common: rows not of high priority are hidden in the short form of the BWA. */
export const BWA_PRIORITIES = [
  { value: "LEAST", i18nKey: "priority.least" },
  { value: "LOW", i18nKey: "priority.low" },
  { value: "MIDDLE", i18nKey: "priority.middle" },
  { value: "HIGH", i18nKey: "priority.high" },
  { value: "HIGHEST", i18nKey: "priority.highest" },
] as const;

export type BwaPriority = (typeof BWA_PRIORITIES)[number]["value"];

/** A row of the business assessment, in the order shown; a record is assigned to the first matching row. */
export interface BwaRow {
  /** Number to display, e.g. 1051. */
  no: string;
  /** Id referred to by formulas and scripts of other rows, e.g. `gesamtleistung`. */
  id: string;
  /** DATEV accounts of the row: `4000-4799`, `4830,4947` or `6300,6800-6855`. */
  accountRange: string;
  /** `=a+b` (formula of row ids) or a Groovy script returning the amount. */
  value: string;
  priority: BwaPriority | null;
  title: string;
  indent: number | null;
  /** Fraction digits, the standard 2 if not given. */
  scale: number | null;
  unit: string;
}

export const EMPTY_BWA_ROW: BwaRow = {
  no: "",
  id: "",
  accountRange: "",
  value: "",
  priority: null,
  title: "",
  indent: null,
  scale: null,
  unit: "",
};

export interface AccountingConfig {
  /** e.g. `10000-12999` */
  debitorsAccountNumberRanges: string;
  /** e.g. `70000-79999` */
  creditorsAccountNumberRanges: string;
  /** Heading of the business assessment. */
  heading: string;
  /** Ids of the rows of the overall performance, the merchandise purchase and the preliminary result. */
  overallPerformance: string;
  merchandisePurchase: string;
  preliminaryResult: string;
  rows: BwaRow[];
}

export const ACCOUNTING_DEFAULTS: AccountingConfig = {
  debitorsAccountNumberRanges: "",
  creditorsAccountNumberRanges: "",
  heading: "",
  overallPerformance: "",
  merchandisePurchase: "",
  preliminaryResult: "",
  rows: [],
};

const PRIORITIES = new Set<string>(BWA_PRIORITIES.map((p) => p.value));

function text(value: unknown): string {
  return typeof value === "string" ? value : "";
}

function numberOrNull(value: unknown): number | null {
  return typeof value === "number" && Number.isFinite(value) ? value : null;
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

/**
 * The settings of the stored value, completed by the defaults. `invalid` if the value isn't a JSON object;
 * the defaults are taken then, as the backend does.
 */
export function parseAccountingConfig(json: string | null | undefined): {
  config: AccountingConfig;
  invalid: boolean;
} {
  if (!json?.trim()) {
    return { config: ACCOUNTING_DEFAULTS, invalid: false };
  }
  try {
    const value: unknown = JSON.parse(json);
    if (!isObject(value)) {
      return { config: ACCOUNTING_DEFAULTS, invalid: true };
    }
    const bwa = isObject(value.businessAssessment) ? value.businessAssessment : {};
    const rows = Array.isArray(bwa.rows) ? bwa.rows.filter(isObject) : [];
    return {
      config: {
        debitorsAccountNumberRanges: text(value.debitorsAccountNumberRanges),
        creditorsAccountNumberRanges: text(value.creditorsAccountNumberRanges),
        heading: text(bwa.heading),
        overallPerformance: text(bwa.overallPerformance),
        merchandisePurchase: text(bwa.merchandisePurchase),
        preliminaryResult: text(bwa.preliminaryResult),
        rows: rows.map((row) => {
          const priority = text(row.priority).toUpperCase();
          return {
            no: text(row.no),
            id: text(row.id),
            accountRange: text(row.accountRange),
            value: text(row.value),
            priority: PRIORITIES.has(priority) ? (priority as BwaPriority) : null,
            title: text(row.title),
            indent: numberOrNull(row.indent),
            // The backend's "not given" is -1.
            scale: numberOrNull(row.scale) != null && (row.scale as number) >= 0 ? (row.scale as number) : null,
            unit: text(row.unit),
          };
        }),
      },
      invalid: false,
    };
  } catch {
    return { config: ACCOUNTING_DEFAULTS, invalid: true };
  }
}

/**
 * Sets the text, if not blank. Not trimmed: the value is written on every keystroke, so a trailing blank
 * typed before the next word must stay. The backend trims.
 */
function putText(target: Record<string, unknown>, key: string, value: string) {
  if (value.trim()) target[key] = value;
}

/** The JSON value of the settings, without empty properties (as the backend writes it). */
export function serializeAccountingConfig(config: AccountingConfig): string {
  const result: Record<string, unknown> = {};
  putText(result, "debitorsAccountNumberRanges", config.debitorsAccountNumberRanges);
  putText(result, "creditorsAccountNumberRanges", config.creditorsAccountNumberRanges);
  const bwa: Record<string, unknown> = {};
  putText(bwa, "heading", config.heading);
  putText(bwa, "overallPerformance", config.overallPerformance);
  putText(bwa, "merchandisePurchase", config.merchandisePurchase);
  putText(bwa, "preliminaryResult", config.preliminaryResult);
  if (config.rows.length > 0) {
    bwa.rows = config.rows.map((row) => {
      const out: Record<string, unknown> = {};
      putText(out, "no", row.no);
      putText(out, "id", row.id);
      putText(out, "accountRange", row.accountRange);
      putText(out, "value", row.value);
      if (row.priority) out.priority = row.priority;
      putText(out, "title", row.title);
      if (row.indent) out.indent = row.indent;
      if (row.scale != null) out.scale = row.scale;
      putText(out, "unit", row.unit);
      return out;
    });
  }
  if (Object.keys(bwa).length > 0) {
    result.businessAssessment = bwa;
  }
  return JSON.stringify(result);
}
