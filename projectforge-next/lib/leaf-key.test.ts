import { describe, expect, it } from "vitest";
import { leafKeyOf } from "./leaf-key";

/** What the generator would have written for keys that are a text and a namespace at once. */
const MESSAGES = new Set([
  "fibu.rechnung.zahlungsZiel._",
  "fibu.rechnung.zahlungsZiel.actual",
  "fibu.rechnung.kostExcelExport._",
  "fibu.rechnung.kostExcelExport.tooltip",
  "fibu.rechnung.offen",
]);
const hasMessage = (key: string) => MESSAGES.has(key);

describe("leafKeyOf", () => {
  it("resolves a key that is a text and a namespace to its exported leaf", () => {
    expect(leafKeyOf("fibu.rechnung.zahlungsZiel", hasMessage)).toBe(
      "fibu.rechnung.zahlungsZiel._"
    );
    expect(leafKeyOf("fibu.rechnung.kostExcelExport", hasMessage)).toBe(
      "fibu.rechnung.kostExcelExport._"
    );
  });

  it("leaves a plain key alone — most keys are one, and `<key>._` would not exist", () => {
    expect(leafKeyOf("fibu.rechnung.offen", hasMessage)).toBe(
      "fibu.rechnung.offen"
    );
  });

  it("leaves a child key alone, which is a leaf already", () => {
    expect(leafKeyOf("fibu.rechnung.zahlungsZiel.actual", hasMessage)).toBe(
      "fibu.rechnung.zahlungsZiel.actual"
    );
  });

  it("hands back a key the catalogue does not know, so the missing text is visible", () => {
    expect(leafKeyOf("gone", hasMessage)).toBe("gone");
  });
});
