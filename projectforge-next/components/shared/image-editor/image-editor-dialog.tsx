"use client";

import { useState } from "react";
import Cropper, { type Area } from "react-easy-crop";
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
import { Label } from "@/components/ui/label";
import { Slider } from "@/components/ui/slider";
import { Spinner } from "@/components/shared/spinner";
import { toast } from "@/lib/toast";
import { cropAndResize, editedImageType, type PixelArea } from "./crop-image";

/** The image to edit: an url the canvas may read (same origin, `blob:`), with the type and name of the file. */
export interface EditableImage {
  src: string;
  type?: string;
  name: string;
}

const MAX_ZOOM = 5;

/**
 * A small image editor: cuts a part with the fixed [aspect] ratio out of an image (drag and zoom) and scales
 * it down to [maxWidth], never up. The result is handed to [onApply] as a file, ready for the upload; nothing is
 * uploaded on cancel. If [onApply] fails, the editor shows the error and stays open.
 */
export function ImageEditorDialog({
  image,
  aspect,
  maxWidth,
  onApply,
  onClose,
}: {
  image: EditableImage;
  /** Width by height of the cut, e.g. 1 for a square. */
  aspect: number;
  /** The width the result is scaled down to at most. */
  maxWidth: number;
  onApply: (file: File) => Promise<void> | void;
  onClose: () => void;
}) {
  const t = useTranslations();
  const [crop, setCrop] = useState({ x: 0, y: 0 });
  const [zoom, setZoom] = useState(1);
  const [area, setArea] = useState<PixelArea | null>(null);
  const [applying, setApplying] = useState(false);

  const apply = async () => {
    if (!area) return;
    setApplying(true);
    try {
      const file = await cropAndResize(
        image.src,
        area,
        maxWidth,
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
          />
        </div>
        <div className="flex flex-col gap-2">
          <Label>{t("imageEditor.zoom")}</Label>
          <Slider
            aria-label={t("imageEditor.zoom")}
            className="h-8"
            min={1}
            max={MAX_ZOOM}
            step={0.05}
            value={[zoom]}
            onValueChange={([value]) => setZoom(value)}
          />
        </div>
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
