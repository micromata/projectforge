import { describe, expect, it } from "vitest";
import { previousPeriodLabels } from "./period-label";

describe("previousPeriodLabels", () => {
  it("names the spanned years for a window not starting in January", () => {
    expect(previousPeriodLabels("2025-11")).toEqual({
      current: "2025/2026",
      prevYear: "2024/2025",
      prevPrevYear: "2023/2024",
      prevPrevPrevYear: "2022/2023",
    });
  });

  it("names the calendar year for a window starting in January", () => {
    expect(previousPeriodLabels("2025-01")).toEqual({
      current: "2025",
      prevYear: "2024",
      prevPrevYear: "2023",
      prevPrevPrevYear: "2022",
    });
  });

  it("handles December and the year change", () => {
    expect(previousPeriodLabels("2026-12").prevYear).toBe("2025/2026");
    expect(previousPeriodLabels("2026-02").prevPrevYear).toBe("2024/2025");
  });

  it("returns empty labels without a month", () => {
    expect(previousPeriodLabels(undefined)).toEqual({
      current: "",
      prevYear: "",
      prevPrevYear: "",
      prevPrevPrevYear: "",
    });
  });
});
