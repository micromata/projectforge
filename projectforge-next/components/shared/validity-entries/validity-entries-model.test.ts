import { describe, expect, it } from "vitest";
import {
  currentEntry,
  sortByValidity,
  validityOf,
} from "./validity-entries-model";

interface Entry {
  id: number;
  validSince?: string | null;
}

const dateOf = (e: Entry) => e.validSince;
const TODAY = "2031-06-15";

describe("sortByValidity", () => {
  it("puts the newest first and entries without a date last", () => {
    const sorted = sortByValidity<Entry>(
      [
        { id: 1, validSince: "2030-01-01" },
        { id: 2, validSince: null },
        { id: 3, validSince: "2032-01-01" },
        { id: 4, validSince: "2031-01-01" },
      ],
      dateOf
    );
    expect(sorted.map((e) => e.id)).toEqual([3, 4, 1, 2]);
  });

  it("leaves the given list as it is", () => {
    const entries: Entry[] = [
      { id: 1, validSince: "2030-01-01" },
      { id: 2, validSince: "2031-01-01" },
    ];
    sortByValidity(entries, dateOf);
    expect(entries.map((e) => e.id)).toEqual([1, 2]);
  });
});

describe("currentEntry", () => {
  const entries: Entry[] = [
    { id: 1, validSince: "2030-01-01" },
    { id: 2, validSince: "2031-01-01" },
    { id: 3, validSince: "2032-01-01" },
  ];

  it("is the latest entry starting on or before today", () => {
    expect(currentEntry(entries, dateOf, TODAY)?.id).toBe(2);
  });

  it("includes an entry starting today", () => {
    expect(currentEntry(entries, dateOf, "2032-01-01")?.id).toBe(3);
  });

  it("is undefined while every entry lies ahead", () => {
    expect(currentEntry(entries, dateOf, "2029-12-31")).toBeUndefined();
  });

  it("ignores entries without a date", () => {
    expect(
      currentEntry([{ id: 1, validSince: null }], dateOf, TODAY)
    ).toBeUndefined();
  });
});

describe("validityOf", () => {
  it("tells past, current and future entries apart", () => {
    const entries: Entry[] = [
      { id: 1, validSince: "2030-01-01" },
      { id: 2, validSince: "2031-01-01" },
      { id: 3, validSince: "2032-01-01" },
    ];
    const current = currentEntry(entries, dateOf, TODAY);
    expect(entries.map((e) => validityOf(e, current, dateOf, TODAY))).toEqual([
      "past",
      "current",
      "future",
    ]);
  });
});
