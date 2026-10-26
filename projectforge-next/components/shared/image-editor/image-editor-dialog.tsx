"use client";

import { useState } from "react";
import Cropper, { type Area, type MediaSize } from "react-easy-crop";
import { useTranslations } from "next-intl";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/shared/spinner";
import { toast } from "@/lib/toast";
import {
  cropAndResize,
  editedImageType,
  targetSize,
  type PixelArea,
} from "./crop-image";
import {
  ASPECTS,
  ImageEditorControls,
  MAX_ZOOM,
  type AspectKey,
} from "./image-editor-controls";

/** The image to edit: an url the canvas may read (same origin, `blob:`), with the type and name of the file. */
export interface EditableImage {
  src: string;
  type?: string;
  name: string;
}

/** The width an image gets by default: large enough for a portrait, small enough not to waste the upload. */
const DEFAULT_WIDTH = 800;

/**
 * A small image editor: cuts a part out of an image (drag and zoom, with a fixed aspect ratio) and scales it
 * to a width, never up. The result is handed to [onApply] as a file, ready for the upload; nothing is
 * uploaded on cancel.
 */
export function ImageEditorDialog({
  image,
  onApply,
  onClose,
}: {
  image: EditableImage;
  onApply: (file: File) => Promise<void> | void;
  onClose: () => void;
}) {
  const t = useTranslations();
  const [crop, setCrop] = useState({ x: 0, y: 0 });
  const [zoom, setZoom] = useState(1);
  const [aspectKey, setAspectKey] = useState<AspectKey>("original");
  const [media, setMedia] = useState<MediaSize | null>(null);
  const [area, setArea] = useState<PixelArea | null>(null);
  const [width, setWidth] = useState("");
  const [applying, setApplying] = useState(false);

  const aspect =
    ASPECTS[aspectKey] ??
    (media ? media.naturalWidth / media.naturalHeight : 1);
  // A width below one pixel (empty, negative, a fraction) is no width: the default applies.
  const requested = Number(width);
  const size = area
    ? targetSize(
        area,
        requested >= 1 ? requested : Math.min(area.width, DEFAULT_WIDTH)
      )
    : null;

  const apply = async () => {
    if (!area || !size) return;
    setApplying(true);
    try {
      const file = await cropAndResize(
        image.src,
        area,
        size.width,
        editedImageType(image.type),
        image.name
      );
      await onApply(file);
      onClose();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : String(error));
    } finally {
      setApplying(false);
    }
  };

  return (
    <Dialog open onOpenChange={(open) => !open && !applying && onClose()}>
      <DialogContent className="sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{t("imageEditor.title")}</DialogTitle>
          <DialogDescription className="sr-only">
            {t("imageEditor.title")}
          </DialogDescription>
        </DialogHeader>
        <div className="relative h-72 w-full overflow-hidden rounded-md bg-muted sm:h-96">
          <Cropper
            image={image.src}
            crop={crop}
            zoom={zoom}
            maxZoom={MAX_ZOOM}
            aspect={aspect}
            onCropChange={setCrop}
            onZoomChange={setZoom}
            onCropComplete={(_: Area, pixels: Area) => setArea(pixels)}
            onMediaLoaded={setMedia}
          />
        </div>
        <ImageEditorControls
          aspectKey={aspectKey}
          onAspectKeyChange={setAspectKey}
          zoom={zoom}
          onZoomChange={setZoom}
          width={width}
          onWidthChange={setWidth}
          size={size}
        />
        <DialogFooter>
          <Button
            type="button"
            variant="outline"
            onClick={onClose}
            disabled={applying}
          >
            {t("cancel")}
          </Button>
          <Button
            type="button"
            onClick={() => void apply()}
            disabled={!area || applying}
          >
            {applying && <Spinner />}
            {t("apply")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
