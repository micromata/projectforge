import { describe, expect, it } from "vitest";
import { freeTextOffer } from "./free-text-offer";

describe("freeTextOffer", () => {
  it("offers nothing while no term is typed", () => {
    expect(freeTextOffer("", ["ACME"])).toBeNull();
    expect(freeTextOffer("   ", [])).toBeNull();
  });

  it("offers the trimmed term when no record is named like it", () => {
    expect(freeTextOffer(" ACME Gmbh ", ["ACME GmbH & Co. KG"])).toEqual({
      text: "ACME Gmbh",
      tooLong: false,
    });
  });

  it("offers nothing when a record is named exactly like the term, case aside", () => {
    expect(freeTextOffer("acme gmbh", ["Other", " ACME GmbH "])).toBeNull();
  });

  it("marks a term longer than the field as not pickable", () => {
    expect(freeTextOffer("abcdef", [], 5)).toEqual({
      text: "abcdef",
      tooLong: true,
    });
    expect(freeTextOffer("abcde", [], 5)?.tooLong).toBe(false);
  });
});
