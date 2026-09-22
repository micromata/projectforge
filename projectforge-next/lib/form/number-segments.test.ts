import { describe, expect, it } from "vitest";
import {
  displaySegment,
  segmentDigits,
  segmentValue,
  splitPastedSegments,
  stepSegment,
  type NumberSegment,
} from "./number-segments";

/** The segments of a cost 1 number, the case these rules exist for. */
const KOST1: NumberSegment[] = [
  { name: "nummernkreis", label: "Nummernkreis", min: 0, max: 9, digits: 1 },
  { name: "bereich", label: "Bereich", min: 0, max: 999, digits: 3 },
  { name: "teilbereich", label: "Teilbereich", min: 0, max: 99, digits: 2 },
  { name: "endziffer", label: "Endziffer", min: 0, max: 99, digits: 2 },
];

describe("displaySegment", () => {
  it("pads a segment wider than one digit", () => {
    expect(displaySegment(1, 3)).toBe("001");
    expect(displaySegment(100, 3)).toBe("100");
  });

  it("leaves a single-digit segment alone — Wicket pads no nummernkreis either", () => {
    expect(displaySegment(6, 1)).toBe("6");
  });

  it("shows nothing for no value, so an empty box stays empty", () => {
    expect(displaySegment(null, 3)).toBe("");
  });

  it("shows a zero as a value of its own", () => {
    expect(displaySegment(0, 2)).toBe("00");
  });
});

describe("segmentDigits", () => {
  it("keeps only digits and stops at the segment's width", () => {
    expect(segmentDigits("12a34", 3)).toBe("123");
    expect(segmentDigits("7", 3)).toBe("7");
    expect(segmentDigits("...", 3)).toBe("");
  });
});

describe("segmentValue", () => {
  it("reads an emptied box as no value, never as 0", () => {
    expect(segmentValue("")).toBeNull();
  });

  it("keeps a typed zero, which is a valid number", () => {
    expect(segmentValue("00")).toBe(0);
  });

  it("drops the padding when reading the number", () => {
    expect(segmentValue("007")).toBe(7);
  });
});

describe("stepSegment", () => {
  const bereich = KOST1[1];

  it("clamps at the upper bound", () => {
    expect(stepSegment(999, bereich, 1)).toBe(999);
  });

  it("clamps at the lower bound", () => {
    expect(stepSegment(0, bereich, -1)).toBe(0);
  });

  it("starts at the lower bound when the box is empty", () => {
    expect(stepSegment(null, bereich, 1)).toBe(1);
    expect(stepSegment(null, bereich, -1)).toBe(0);
  });

  it("steps by one otherwise", () => {
    expect(stepSegment(100, bereich, 1)).toBe(101);
  });
});

describe("splitPastedSegments", () => {
  it("fills the whole group from a cost number", () => {
    expect([...splitPastedSegments("6.100.01.02", KOST1)]).toEqual([
      ["nummernkreis", 6],
      ["bereich", 100],
      ["teilbereich", 1],
      ["endziffer", 2],
    ]);
  });

  it("splits at any non-digit, not just at the dot", () => {
    expect(splitPastedSegments("6-100-01-02", KOST1).get("bereich")).toBe(100);
    expect(splitPastedSegments("6 100 01 02", KOST1).get("endziffer")).toBe(2);
  });

  it("leaves the boxes a short paste doesn't reach untouched", () => {
    const values = splitPastedSegments("6.100", KOST1);
    expect([...values.keys()]).toEqual(["nummernkreis", "bereich"]);
  });

  it("truncates a part to the segment's width instead of storing a number out of range", () => {
    expect(splitPastedSegments("6.1000", KOST1).get("bereich")).toBe(100);
  });

  it("ignores parts beyond the last segment", () => {
    expect(splitPastedSegments("6.100.01.02.99", KOST1).size).toBe(4);
  });
});
