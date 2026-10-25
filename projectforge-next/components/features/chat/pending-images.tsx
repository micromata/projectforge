"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Cancel01Icon } from "@hugeicons/core-free-icons";
import { Spinner } from "@/components/shared/spinner";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import type { PendingImage } from "./types";

/** The previews of the images of the message being written, each removable. */
export function PendingImages({
  images,
  onRemove,
}: {
  images: PendingImage[];
  onRemove: (key: string) => void;
}) {
  const t = useTranslations();
  if (images.length === 0) return null;
  return (
    <ul className="flex flex-wrap gap-2 px-2 pt-2">
      {images.map((image) => (
        <li
          key={image.key}
          className={cn(
            "relative size-16 overflow-hidden rounded-md border",
            image.error && "border-destructive"
          )}
        >
          {/* eslint-disable-next-line @next/next/no-img-element -- local object url, not optimizable */}
          <img
            src={image.previewUrl}
            alt={image.name}
            className="size-full object-cover"
          />
          {!image.fileId && !image.error && (
            <div className="absolute inset-0 flex items-center justify-center bg-background/60">
              <Spinner className="h-4 w-4 border-2" />
              <span className="sr-only">{t("chat.image.uploading")}</span>
            </div>
          )}
          {image.error && (
            <div className="absolute inset-x-0 bottom-0 truncate bg-background/90 px-1 text-[0.625rem] text-destructive">
              {image.error}
            </div>
          )}
          <Button
            type="button"
            variant="secondary"
            size="icon-xs"
            className="absolute top-0.5 right-0.5"
            aria-label={`${t("chat.image.remove")}: ${image.name}`}
            onClick={() => onRemove(image.key)}
          >
            <HugeiconsIcon icon={Cancel01Icon} />
          </Button>
        </li>
      ))}
    </ul>
  );
}
