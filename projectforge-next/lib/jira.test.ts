import { describe, expect, it } from "vitest";
import { findJiraIssues, splitJiraSegments } from "./jira";

describe("findJiraIssues", () => {
  it("finds keys with at least two leading uppercase letters", () => {
    expect(findJiraIssues("Worked on PF-1, PF1DF-2 and PROJECT2-123.")).toEqual([
      "PF-1",
      "PF1DF-2",
      "PROJECT2-123",
    ]);
  });

  it("ignores a single letter prefix such as a postal code", () => {
    expect(findJiraIssues("D-34286 Spangenberg")).toEqual([]);
    expect(findJiraIssues("P-0")).toEqual([]);
  });
});

describe("splitJiraSegments", () => {
  it("keeps a postal code as plain text", () => {
    expect(splitJiraSegments("D-34286 Spangenberg, see PF-7")).toEqual([
      { type: "text", value: "D-34286 Spangenberg, see " },
      { type: "issue", value: "PF-7" },
    ]);
  });
});
