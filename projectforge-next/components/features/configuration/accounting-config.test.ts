import { describe, expect, it } from "vitest";
import {
  ACCOUNTING_DEFAULTS,
  EMPTY_BWA_ROW,
  parseAccountingConfig,
  serializeAccountingConfig,
} from "./accounting-config";

const STORED = JSON.stringify({
  debitorsAccountNumberRanges: "10000-12999",
  creditorsAccountNumberRanges: "70000-79999",
  businessAssessment: {
    heading: "BWA",
    overallPerformance: "gesamtleistung",
    rows: [
      { no: "1020", id: "umsatzErloese", accountRange: "4000-4799", priority: "MIDDLE", title: "Umsatzerlöse", indent: 1 },
      { no: "1051", id: "gesamtleistung", value: "=umsatzErloese", priority: "HIGH", title: "Gesamtleistung" },
      { id: "quote", value: "if (a) {\n  return 1\n}", priority: "HIGH", scale: 4, unit: "%" },
    ],
  },
});

describe("parseAccountingConfig", () => {
  it("reads ranges, key rows and rows", () => {
    const { config, invalid } = parseAccountingConfig(STORED);
    expect(invalid).toBe(false);
    expect(config.debitorsAccountNumberRanges).toBe("10000-12999");
    expect(config.overallPerformance).toBe("gesamtleistung");
    expect(config.merchandisePurchase).toBe("");
    expect(config.rows).toHaveLength(3);
    expect(config.rows[0]).toEqual({
      ...EMPTY_BWA_ROW,
      no: "1020",
      id: "umsatzErloese",
      accountRange: "4000-4799",
      priority: "MIDDLE",
      title: "Umsatzerlöse",
      indent: 1,
    });
    expect(config.rows[2].scale).toBe(4);
  });

  it("reads the priority case-insensitively, as the backend does", () => {
    const json = '{"businessAssessment":{"rows":[{"priority":"high","scale":-1}]}}';
    const row = parseAccountingConfig(json).config.rows[0];
    expect(row.priority).toBe("HIGH");
    expect(row.scale).toBeNull();
  });

  it("takes the defaults for an empty or invalid value", () => {
    expect(parseAccountingConfig("")).toEqual({ config: ACCOUNTING_DEFAULTS, invalid: false });
    expect(parseAccountingConfig("[]")).toEqual({ config: ACCOUNTING_DEFAULTS, invalid: true });
  });
});

describe("serializeAccountingConfig", () => {
  it("writes the stored value back unchanged", () => {
    expect(serializeAccountingConfig(parseAccountingConfig(STORED).config)).toBe(STORED);
  });

  it("omits empty settings", () => {
    expect(serializeAccountingConfig(ACCOUNTING_DEFAULTS)).toBe("{}");
    expect(
      serializeAccountingConfig({ ...ACCOUNTING_DEFAULTS, rows: [{ ...EMPTY_BWA_ROW, title: "Leer " }] })
    ).toBe('{"businessAssessment":{"rows":[{"title":"Leer "}]}}');
  });
});
