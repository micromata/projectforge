import { describe, expect, it } from "vitest";
import {
  isMerlinCsvWritable,
  parseMerlinCsv,
  writeMerlinCsv,
} from "./merlin-csv";

describe("parseMerlinCsv", () => {
  it("reads values as the backend does (MerlinVariableTest)", () => {
    expect(parseMerlinCsv(null)).toEqual([]);
    expect(parseMerlinCsv("")).toEqual([]);
    expect(parseMerlinCsv("one")).toEqual(["one"]);
    expect(parseMerlinCsv('"one a, one b"')).toEqual(["one a, one b"]);
    expect(parseMerlinCsv("one, two")).toEqual(["one", "two"]);
    expect(parseMerlinCsv('"one a, one b", two')).toEqual([
      "one a, one b",
      "two",
    ]);
  });

  it("splits at ; and : too and skips an empty unquoted value", () => {
    expect(parseMerlinCsv("a; b: c")).toEqual(["a", "b", "c"]);
    expect(parseMerlinCsv("a, , c")).toEqual(["a", "c"]);
    expect(parseMerlinCsv('a, "", c')).toEqual(["a", "", "c"]);
  });
});

describe("writeMerlinCsv", () => {
  it("quotes what would be split or lost, so it reads back as given", () => {
    const values = [
      "Lieber",
      "Liebe, ",
      "",
      "a: b",
      'say "hi", you',
      '"quoted"',
      "it's",
    ];
    const csv = writeMerlinCsv(values);
    expect(csv).toBe(
      `Lieber, "Liebe,", "", "a: b", 'say "hi", you', '"quoted"', it's`
    );
    expect(parseMerlinCsv(csv)).toEqual(values.map((it) => it.trim()));
  });

  it("refuses a value needing quotes that contains both kinds", () => {
    expect(isMerlinCsvWritable(`"it's", ok`)).toBe(false);
    expect(isMerlinCsvWritable(`it's "ok"`)).toBe(true);
  });
});
