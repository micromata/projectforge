import { describe, expect, it } from "vitest";
import { clampToViewport } from "./panel-position";

const panel = { width: 400, height: 300 };
const viewport = { width: 1200, height: 800 };

describe("clampToViewport", () => {
  it("keeps a position inside the viewport as it is", () => {
    const position = { x: 100, y: 200 };
    expect(clampToViewport(position, panel, viewport)).toBe(position);
  });

  it("moves a panel beyond the right and bottom edge back in", () => {
    expect(clampToViewport({ x: 1000, y: 700 }, panel, viewport)).toEqual({
      x: 800,
      y: 500,
    });
  });

  it("moves a panel beyond the left and top edge back in", () => {
    expect(clampToViewport({ x: -50, y: -10 }, panel, viewport)).toEqual({
      x: 0,
      y: 0,
    });
  });

  it("pins the title bar to the top-left when the panel is larger than the viewport", () => {
    expect(
      clampToViewport({ x: 30, y: 30 }, panel, { width: 300, height: 200 })
    ).toEqual({ x: 0, y: 0 });
  });
});
