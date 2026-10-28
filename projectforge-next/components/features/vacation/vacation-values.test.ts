import { describe, expect, it } from "vitest";
import { toFormValues } from "./vacation-values";

describe("toFormValues", () => {
  it("keeps the begin of a vacation shown in full", () => {
    expect(toFormValues({ id: 1, startDate: "2031-01-02" }).startDate).toBe(
      "2031-01-02"
    );
  });

  it("leaves the begin empty if it isn't told (a group mate's vacation shown from 1 January on)", () => {
    const values = toFormValues({
      id: 1,
      startDate: "2031-01-01",
      endDate: "2031-01-03",
      startHidden: true,
    });
    expect(values.startDate).toBeNull();
    expect(values.endDate).toBe("2031-01-03");
  });
});
