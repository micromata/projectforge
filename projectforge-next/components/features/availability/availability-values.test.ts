import { describe, expect, it } from "vitest";
import { toFormValues } from "./availability-values";

describe("toFormValues", () => {
  it("keeps the begin of an entry shown in full", () => {
    const values = toFormValues({
      id: 1,
      startDate: "2031-03-01",
      endDate: "2031-03-31",
    });
    expect(values.startDate).toBe("2031-03-01");
    expect(values.endDate).toBe("2031-03-31");
  });

  it("leaves the begin empty if it isn't told (an entry of somebody else shown from today on)", () => {
    const values = toFormValues({
      id: 1,
      startDate: "2031-03-15",
      endDate: "2031-03-31",
      startHidden: true,
    });
    expect(values.startDate).toBeNull();
    expect(values.endDate).toBe("2031-03-31");
  });
});
