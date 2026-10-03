import { describe, expect, it } from "vitest";
import { recordsHref, visibleRows } from "./rows";
import type { ReportRow } from "./types";

const row = (no: string, priority?: ReportRow["priority"]): ReportRow => ({
  no,
  indent: 0,
  scale: 2,
  amounts: [],
  priority,
});

describe("visibleRows", () => {
  const rows = [
    row("1", "HIGHEST"),
    row("2", "HIGH"),
    row("3", "MIDDLE"),
    row("4", "LOW"),
    row("5", null),
  ];

  it("keeps only rows of priority HIGH and above by default", () => {
    expect(visibleRows(rows, false).map((r) => r.no)).toEqual(["1", "2"]);
  });

  it("keeps all rows if asked to", () => {
    expect(visibleRows(rows, true)).toHaveLength(5);
  });
});

describe("recordsHref", () => {
  it("links the report's records, without the basePath", () => {
    expect(recordsHref("ACME - Other")).toBe(
      "/accounting-record?reportId=ACME+-+Other"
    );
  });

  it("adds the BWA row", () => {
    expect(recordsHref("ACME", "1051")).toBe(
      "/accounting-record?reportId=ACME&businessAssessmentRowId=1051"
    );
  });
});
