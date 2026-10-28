import { describe, expect, it } from "vitest";
import {
  parseContractTypesConfig,
  serializeContractTypesConfig,
} from "./contract-types-config";

const STORED = '[{"value":"NDA","label":"Non-disclosure agreement"},{"value":"AV","label":"Arbeitsvertrag"}]';

describe("contract types config", () => {
  it("reads and writes the stored value unchanged", () => {
    const { entries, invalid } = parseContractTypesConfig(STORED);
    expect(invalid).toBe(false);
    expect(entries).toEqual([
      { value: "NDA", label: "Non-disclosure agreement" },
      { value: "AV", label: "Arbeitsvertrag" },
    ]);
    expect(serializeContractTypesConfig(entries)).toBe(STORED);
  });

  it("handles empty and invalid values", () => {
    expect(parseContractTypesConfig(null)).toEqual({ entries: [], invalid: false });
    expect(parseContractTypesConfig("{}")).toEqual({ entries: [], invalid: true });
    expect(serializeContractTypesConfig([])).toBe("");
  });
});
