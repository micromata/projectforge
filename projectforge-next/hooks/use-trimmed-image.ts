"use client";

import { useEffect, useState } from "react";

/** Alpha up to this counts as transparent: antialiasing fringes and near-invisible noise of an export. */
const ALPHA_THRESHOLD = 8;

export interface OpaqueBox {
  x: number;
  y: number;
  width: number;
  height: number;
}

/**
 * The smallest box holding every visible pixel of RGBA `data` (as `ImageData.data`), or null where the
 * image is transparent throughout — there is nothing to crop to then.
 */
export function opaqueBox(
  data: Uint8ClampedArray,
  width: number,
  height: number
): OpaqueBox | null {
  let minX = width;
  let minY = height;
  let maxX = -1;
  let maxY = -1;
  for (let y = 0; y < height; y++) {
    for (let x = 0; x < width; x++) {
      if (data[(y * width + x) * 4 + 3] > ALPHA_THRESHOLD) {
        if (x < minX) minX = x;
        if (x > maxX) maxX = x;
        if (y < minY) minY = y;
        if (y > maxY) maxY = y;
      }
    }
  }
  if (maxX < 0) return null;
  return { x: minX, y: minY, width: maxX - minX + 1, height: maxY - minY + 1 };
}

export interface TrimmedImage {
  src: string;
  /** The height in px the cropped image is to be shown at; undefined where nothing was cropped. */
  height?: number;
}

/**
 * `url` with its transparent margin cut off, as a blob url; the url itself where there is no margin.
 *
 * Meant for images an admin supplies (the customer logo): one exported with a transparent border would
 * otherwise sit indented next to everything aligned to the same edge. Only transparent pixels are
 * trimmed, an opaque background stays.
 *
 * The crop is not enlarged: `height` is what the visible part measured in the original, shown with a
 * height of at most `maxHeight`. So the image keeps its size and only loses the margin; centring the
 * cropped image vertically then centres what is visible of it.
 *
 * Until the image is measured, and whenever it can't be (load error, a canvas that is not readable), the
 * original url is returned, so the caller's `<img onError>` still sees a broken image. Reading the pixels
 * needs a same-origin image (`/rsPublic` is), showing the result needs `img-src blob:` in the CSP.
 */
export function useTrimmedImage(
  url: string | undefined,
  maxHeight: number
): TrimmedImage | undefined {
  const [trimmed, setTrimmed] = useState<{ url: string } & TrimmedImage>();

  useEffect(() => {
    if (!url) return;
    let cancelled = false;
    let blobUrl: string | undefined;
    const image = new Image();
    image.onload = () => {
      if (cancelled) return;
      try {
        const { naturalWidth: width, naturalHeight: height } = image;
        const canvas = document.createElement("canvas");
        canvas.width = width;
        canvas.height = height;
        const context = canvas.getContext("2d");
        if (!context) return;
        context.drawImage(image, 0, 0);
        const box = opaqueBox(
          context.getImageData(0, 0, width, height).data,
          width,
          height
        );
        if (!box || (box.width === width && box.height === height)) return;
        const cropped = document.createElement("canvas");
        cropped.width = box.width;
        cropped.height = box.height;
        cropped
          .getContext("2d")
          ?.drawImage(
            canvas,
            box.x,
            box.y,
            box.width,
            box.height,
            0,
            0,
            box.width,
            box.height
          );
        cropped.toBlob((blob) => {
          if (!blob || cancelled) return;
          blobUrl = URL.createObjectURL(blob);
          const scale = Math.min(1, maxHeight / height);
          setTrimmed({ url, src: blobUrl, height: box.height * scale });
        });
      } catch {
        // A tainted canvas (getImageData throws) or no canvas support: keep the image as it is.
      }
    };
    image.src = url;
    return () => {
      cancelled = true;
      if (blobUrl) URL.revokeObjectURL(blobUrl);
    };
  }, [url, maxHeight]);

  if (!url) return undefined;
  // Keyed by url, so a changed url never shows the crop of the previous image.
  return trimmed && trimmed.url === url ? trimmed : { src: url };
}
