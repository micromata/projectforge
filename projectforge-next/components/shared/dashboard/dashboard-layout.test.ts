import { describe, expect, it } from "vitest";
import {
  isDefaultLayout,
  moveTile,
  resolveTiles,
  updateTile,
  type DashboardTileDefaults,
} from "./dashboard-layout";

const DEFAULTS: DashboardTileDefaults[] = [
  { id: "a", defaultWidth: "half" },
  { id: "b", defaultWidth: "half", defaultHeight: "S" },
  { id: "c" },
];

describe("resolveTiles", () => {
  it("returns the defaults without a stored layout", () => {
    expect(resolveTiles(DEFAULTS, undefined)).toEqual([
      { id: "a", width: "half", height: "M", hidden: false },
      { id: "b", width: "half", height: "S", hidden: false },
      { id: "c", width: "full", height: "M", hidden: false },
    ]);
  });

  it("follows the stored order and sizes, drops unknown ids, appends new tiles", () => {
    const tiles = resolveTiles(DEFAULTS, [
      { id: "b", width: "full", height: "L", hidden: true },
      { id: "gone", width: "third" },
      { id: "a" },
      { id: "b", width: "third" },
    ]);
    expect(tiles.map((tile) => tile.id)).toEqual(["b", "a", "c"]);
    expect(tiles[0]).toEqual({
      id: "b",
      width: "full",
      height: "L",
      hidden: true,
    });
    expect(tiles[1]).toEqual({
      id: "a",
      width: "half",
      height: "M",
      hidden: false,
    });
  });

  it("falls back to the defaults for unknown sizes", () => {
    const [tile] = resolveTiles(DEFAULTS, [
      // Sizes of a newer or tampered client.
      { id: "a", width: "huge" as never, height: "XL" as never },
    ]);
    expect(tile).toEqual({
      id: "a",
      width: "half",
      height: "M",
      hidden: false,
    });
  });
});

describe("editing", () => {
  it("moves a tile and detects the default layout", () => {
    const tiles = resolveTiles(DEFAULTS, undefined);
    const moved = moveTile(tiles, "c", "a");
    expect(moved.map((tile) => tile.id)).toEqual(["c", "a", "b"]);
    expect(isDefaultLayout(DEFAULTS, moved)).toBe(false);
    expect(isDefaultLayout(DEFAULTS, moveTile(moved, "c", "b"))).toBe(true);
  });

  it("updates one tile only", () => {
    const tiles = updateTile(resolveTiles(DEFAULTS, undefined), "b", {
      hidden: true,
    });
    expect(tiles.map((tile) => tile.hidden)).toEqual([false, true, false]);
  });
});
