import { describe, expect, it } from "vitest";
import {
  cellClassFor,
  COST_CELL_CLASS,
  NUMERIC_CELL_CLASS,
} from "./cell-styles";

describe("cellClassFor", () => {
  it("gives numbers the monospaced tabular look", () => {
    expect(cellClassFor("numeric")).toBe(NUMERIC_CELL_CLASS);
  });

  it("gives cost numbers the monospaced look", () => {
    expect(cellClassFor("cost")).toBe(COST_CELL_CLASS);
  });

  it("appends extra classes and skips falsy ones", () => {
    expect(cellClassFor("cost", "font-semibold", false, undefined)).toBe(
      "font-mono font-semibold"
    );
  });
});
