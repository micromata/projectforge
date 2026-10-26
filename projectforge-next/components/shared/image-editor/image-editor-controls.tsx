"use client";

import { useTranslations } from "next-intl";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Slider } from "@/components/ui/slider";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";

export const ASPECTS = {
  original: null,
  "1:1": 1,
  "3:4": 3 / 4,
  "4:3": 4 / 3,
} as const;
export type AspectKey = keyof typeof ASPECTS;

export const MAX_ZOOM = 5;

/** The settings of the image editor below the cropper: aspect ratio, zoom and the width of the result. */
export function ImageEditorControls({
  aspectKey,
  onAspectKeyChange,
  zoom,
  onZoomChange,
  width,
  onWidthChange,
  size,
}: {
  aspectKey: AspectKey;
  onAspectKeyChange: (key: AspectKey) => void;
  zoom: number;
  onZoomChange: (zoom: number) => void;
  width: string;
  onWidthChange: (width: string) => void;
  /** The size of the result, once the cropper reported its area. */
  size: { width: number; height: number } | null;
}) {
  const t = useTranslations();
  return (
    <div className="grid gap-4 sm:grid-cols-2">
      <div className="flex flex-col gap-2">
        <Label>{t("imageEditor.aspectRatio")}</Label>
        <ToggleGroup
          type="single"
          variant="outline"
          size="sm"
          spacing={0}
          value={aspectKey}
          aria-label={t("imageEditor.aspectRatio")}
          // Radix reports "" when the active item is clicked again: keep the ratio then.
          onValueChange={(value) =>
            value && onAspectKeyChange(value as AspectKey)
          }
        >
          {(Object.keys(ASPECTS) as AspectKey[]).map((key) => (
            <ToggleGroupItem key={key} value={key} className="px-3">
              {key === "original" ? t("imageEditor.aspectRatioOriginal") : key}
            </ToggleGroupItem>
          ))}
        </ToggleGroup>
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
          onValueChange={([value]) => onZoomChange(value)}
        />
      </div>
      <div className="flex flex-col gap-2">
        <Label htmlFor="image-editor-width">{t("imageEditor.width")}</Label>
        <div className="flex items-center gap-3">
          <Input
            id="image-editor-width"
            type="number"
            inputMode="numeric"
            min={1}
            className="w-28"
            placeholder={size ? String(size.width) : undefined}
            value={width}
            onChange={(event) => onWidthChange(event.target.value)}
          />
          {size && (
            <span className="text-sm text-muted-foreground tabular-nums">
              {size.width} × {size.height} px
            </span>
          )}
        </div>
      </div>
    </div>
  );
}
