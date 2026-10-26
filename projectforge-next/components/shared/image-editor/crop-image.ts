/** A rectangle of an image in its natural pixels, as the cropper reports it. */
export interface PixelArea {
  x: number;
  y: number;
  width: number;
  height: number;
}

const JPEG_QUALITY = 0.92;

function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const image = new Image();
    image.onload = () => resolve(image);
    image.onerror = () => reject(new Error(`Image not loadable: ${src}`));
    image.src = src;
  });
}

/**
 * The format of the edited image: a JPEG stays a JPEG, everything else becomes a PNG, which keeps the
 * transparency a PNG or GIF (or an image of unknown type) may have. (The backend converts and shrinks the upload
 * anyway, an animation of a GIF is lost either way.)
 */
export function editedImageType(sourceType: string | undefined): string {
  return sourceType === "image/jpeg" ? "image/jpeg" : "image/png";
}

/** The size of the result: the given width, the height following the area, never larger than the area. */
export function targetSize(
  area: PixelArea,
  width: number
): { width: number; height: number } {
  const w = Math.max(1, Math.min(Math.round(width), Math.round(area.width)));
  return {
    width: w,
    height: Math.max(1, Math.round((area.height * w) / area.width)),
  };
}

/** Cuts the area out of the image at `src` and scales it to `width` (see [targetSize]), as a file to upload. */
export async function cropAndResize(
  src: string,
  area: PixelArea,
  width: number,
  type: string,
  fileName: string
): Promise<File> {
  const image = await loadImage(src);
  const size = targetSize(area, width);
  const canvas = document.createElement("canvas");
  canvas.width = size.width;
  canvas.height = size.height;
  const context = canvas.getContext("2d");
  if (!context) throw new Error("Canvas not available");
  context.imageSmoothingQuality = "high";
  if (type === "image/jpeg") {
    // A JPEG has no transparency: transparent pixels would turn black.
    context.fillStyle = "white";
    context.fillRect(0, 0, size.width, size.height);
  }
  context.drawImage(
    image,
    area.x,
    area.y,
    area.width,
    area.height,
    0,
    0,
    size.width,
    size.height
  );
  const blob = await new Promise<Blob | null>((resolve) =>
    canvas.toBlob(resolve, type, JPEG_QUALITY)
  );
  if (!blob) throw new Error("Image not encodable");
  const extension = type === "image/png" ? "png" : "jpg";
  const baseName = fileName.replace(/\.[^.]*$/, "") || "image";
  return new File([blob], `${baseName}.${extension}`, { type });
}
