import { describe, expect, it } from "vitest";
import {
  ABSENCE_KINDS,
  absenceKindOfStatus,
  sortAbsenceKinds,
} from "./absence-kinds";

describe("sortAbsenceKinds", () => {
  it("returns the known kinds in their canonical order, each once", () => {
    expect(
      sortAbsenceKinds(["REMOTE_REACHABLE", "VACATION", "UNKNOWN", "VACATION"])
    ).toEqual(["VACATION", "REMOTE_REACHABLE"]);
    expect(sortAbsenceKinds(null)).toEqual([]);
    expect(sortAbsenceKinds([...ABSENCE_KINDS].reverse())).toEqual([
      ...ABSENCE_KINDS,
    ]);
  });
});

describe("absenceKindOfStatus", () => {
  it("maps an availability status to the kind of the same name, a vacation is none", () => {
    expect(absenceKindOfStatus("NOT_AVAILABLE")).toBe("NOT_AVAILABLE");
    expect(absenceKindOfStatus("REACHABLE_IF_NEEDED")).toBe(
      "REACHABLE_IF_NEEDED"
    );
    expect(absenceKindOfStatus("VACATION")).toBeNull();
    expect(absenceKindOfStatus(undefined)).toBeNull();
  });
});
