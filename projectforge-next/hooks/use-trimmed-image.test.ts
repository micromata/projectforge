import { describe, expect, it } from "vitest";
import { opaqueBox } from "./use-trimmed-image";

/** An RGBA image of `width` × `height`, transparent except for the given opaque pixels. */
function image(width: number, height: number, opaque: [number, number][]) {
  const data = new Uint8ClampedArray(width * height * 4);
  for (const [x, y] of opaque) data[(y * width + x) * 4 + 3] = 255;
  return data;
}

/** Only the measuring: loading and cropping need a real canvas, which jsdom doesn't have. */
describe("opaqueBox", () => {
  it("finds the visible pixels inside a transparent margin", () => {
    expect(
      opaqueBox(
        image(10, 6, [
          [2, 1],
          [7, 4],
        ]),
        10,
        6
      )
    ).toEqual({ x: 2, y: 1, width: 6, height: 4 });
  });

  it("is the whole image where there is no margin", () => {
    expect(
      opaqueBox(
        image(3, 2, [
          [0, 0],
          [2, 1],
        ]),
        3,
        2
      )
    ).toEqual({ x: 0, y: 0, width: 3, height: 2 });
  });

  it("is null for an image transparent throughout", () => {
    expect(opaqueBox(image(4, 4, []), 4, 4)).toBeNull();
  });

  it("ignores nearly transparent pixels", () => {
    const data = image(4, 4, [[1, 1]]);
    data[(3 * 4 + 3) * 4 + 3] = 8;
    expect(opaqueBox(data, 4, 4)).toEqual({ x: 1, y: 1, width: 1, height: 1 });
  });
});
