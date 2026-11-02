import { describe, expect, it } from "vitest";
import { kostDigits } from "./kost-zuweisung";

describe("kostDigits", () => {
  it("drops the dots of the number the display name starts with", () => {
    expect(
      kostDigits({
        id: 1,
        displayName: "5.020.01.02: Development - Project X",
      })
    ).toBe("50200102");
  });

  it("keeps the digits of the project's name out", () => {
    expect(
      kostDigits({
        id: 1,
        displayName: "5.020.01.02: Development - Release 2025",
      })
    ).toBe("50200102");
  });

  it("prefers the formatted number where the reference carries it", () => {
    expect(
      kostDigits({
        id: 1,
        displayName: "Development - Project X",
        formattedNumber: "4.711.03.12",
      })
    ).toBe("47110312");
  });

  it("reads a cost 1 unit the same way", () => {
    expect(kostDigits({ id: 1, displayName: "1.400.01.00: Revenue" })).toBe(
      "14000100"
    );
  });

  it("takes a display name that is the number alone", () => {
    expect(kostDigits({ id: 1, displayName: "5.020.01.02" })).toBe("50200102");
  });

  it("has nothing to copy without a unit or for an unknown one", () => {
    expect(kostDigits(null)).toBeNull();
    expect(kostDigits(undefined)).toBeNull();
    expect(kostDigits({ id: 1 })).toBeNull();
    expect(kostDigits({ id: 1, displayName: "?.???.??.??" })).toBeNull();
  });
});
