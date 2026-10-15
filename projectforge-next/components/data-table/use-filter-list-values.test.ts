import { describe, expect, it } from "vitest";
import { otherFilterEntries } from "./use-filter-list-values";

describe("otherFilterEntries", () => {
  it("leaves out the field's own entry and keeps the others in a stable order", () => {
    const entries = otherFilterEntries(
      {
        projects: { values: ["17"] },
        status: { values: ["BEAUFTRAGT"] },
        customers: { values: ["k:473"] },
      },
      "projects"
    );
    expect(entries).toEqual([
      { field: "customers", value: { values: ["k:473"] } },
      { field: "status", value: { values: ["BEAUFTRAGT"] } },
    ]);
  });
});
