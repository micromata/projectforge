import { describe, expect, it } from "vitest";
import {
  kostNumberOf,
  parseLanesAndPlanesConfig,
  splitEntries,
} from "./lanes-and-planes-config";

describe("splitEntries", () => {
  it("splits at commas, semicolons and line breaks", () => {
    expect(splitEntries("1.010.01.00;1.020.01.00, 5.*.09\n5.*.20\r\n")).toEqual(
      ["1.010.01.00", "1.020.01.00", "5.*.09", "5.*.20"]
    );
  });

  it("drops blanks and empty entries", () => {
    expect(splitEntries("  ;; ,\n  1.010.01.00  ,,")).toEqual(["1.010.01.00"]);
    expect(splitEntries("")).toEqual([]);
  });
});

describe("kostNumberOf", () => {
  it("takes the formatted number of a display name", () => {
    expect(kostNumberOf("5.123.45.09: Diverses - Project - Customer")).toBe(
      "5.123.45.09"
    );
    expect(kostNumberOf("1.010.01.00")).toBe("1.010.01.00");
  });

  it("is null without a number", () => {
    expect(kostNumberOf("Diverses")).toBeNull();
  });
});

describe("parseLanesAndPlanesConfig", () => {
  it("completes the additional users and the missing settings", () => {
    const { config, invalid } = parseLanesAndPlanesConfig(
      '{"accountingInvoiceProfileIds":[1],"additionalUsers":[{"email":"a@example.org"}]}'
    );
    expect(invalid).toBe(false);
    expect(config.additionalUsers).toEqual([
      { email: "a@example.org", firstName: "", lastName: "" },
    ]);
    expect(config.generalKost1).toEqual([]);
  });
});
