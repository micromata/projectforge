import { describe, expect, it } from "vitest";
import { ceilToGrid, floorToGrid, timeGridOf } from "./timesheet-time-grid";

const at = (hour: number, minute: number, second = 0) =>
  new Date(2031, 2, 10, hour, minute, second).getTime();

describe("time grid", () => {
  it("rounds the start down and the end up to the grid", () => {
    expect(floorToGrid(at(10, 7), 15)).toBe(at(10, 0));
    expect(ceilToGrid(at(10, 31), 15)).toBe(at(10, 45));
    expect(ceilToGrid(at(10, 46), 15)).toBe(at(11, 0));
  });

  it("keeps a time that is on the grid", () => {
    expect(floorToGrid(at(10, 30), 15)).toBe(at(10, 30));
    expect(ceilToGrid(at(10, 30), 15)).toBe(at(10, 30));
    expect(ceilToGrid(at(10, 30, 1), 15)).toBe(at(10, 45));
  });

  it("takes a valid grid and falls back to 5 minutes", () => {
    expect([5, 10, 15, 20, 30].map(timeGridOf)).toEqual([5, 10, 15, 20, 30]);
    expect([undefined, null, 0, 7, 45].map(timeGridOf)).toEqual([
      5, 5, 5, 5, 5,
    ]);
  });
});
