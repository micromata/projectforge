"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Cancel01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import type { ChatLinkPreview } from "@/lib/rs/chat-types";
import { cn } from "@/lib/utils";

/** The smallest thumbnail shown as a banner above the text, and its smallest ratio of width to height. */
const BANNER_MIN_WIDTH = 360;
const BANNER_MIN_RATIO = 1.4;

/**
 * The preview of the first link of a message (as in other chats): image, site, title and description of the linked
 * page, a click opens it. All of it plain text, the image is served by the chat. A large, wide image is shown as a
 * banner above the text, a small or square one (a logo) beside it, never enlarged. [compact] (while writing) always
 * puts it beside the text. The author may remove the preview.
 */
export function LinkPreviewCard({
  preview,
  imageSrc,
  compact = false,
  onRemove,
}: {
  preview: ChatLinkPreview;
  /** The url of the thumbnail, null for none. */
  imageSrc: string | null;
  compact?: boolean;
  /** Null if the user may not remove it. */
  onRemove: (() => void) | null;
}) {
  const t = useTranslations();
  const [imageFailed, setImageFailed] = useState(false);
  const host = hostOf(preview.url);
  // The site only if it adds something to the title (e.g. not "ProjectForge" above "ProjectForge").
  const site =
    preview.siteName &&
    preview.siteName.toLowerCase() !== preview.title?.toLowerCase()
      ? preview.siteName
      : host;
  const image = imageSrc && !imageFailed ? imageSrc : null;
  const width = preview.imageWidth ?? 0;
  const height = preview.imageHeight ?? 0;
  const banner =
    image != null &&
    !compact &&
    width >= BANNER_MIN_WIDTH &&
    height > 0 &&
    width / height >= BANNER_MIN_RATIO;
  return (
    <div className="relative mt-1 w-full max-w-sm overflow-hidden rounded-md border bg-card text-card-foreground">
      <a
        href={preview.url}
        target="_blank"
        rel="noopener noreferrer"
        className={cn("block hover:bg-accent/50", !banner && "flex gap-3 p-2")}
      >
        {image && (
          // eslint-disable-next-line @next/next/no-img-element -- served by Spring with the session, no optimizer
          <img
            src={image}
            alt=""
            loading="lazy"
            onError={() => setImageFailed(true)}
            className={
              banner
                ? "max-h-44 w-full object-cover"
                : "size-16 shrink-0 rounded bg-muted object-cover"
            }
            style={banner ? { aspectRatio: `${width} / ${height}` } : undefined}
          />
        )}
        <div
          className={cn(
            "flex min-w-0 flex-col gap-0.5",
            banner && "px-3 py-2",
            onRemove && !banner && "pr-6"
          )}
        >
          <span className="truncate text-xs text-muted-foreground">{site}</span>
          {preview.title && (
            <span className="line-clamp-2 text-sm font-medium">
              {preview.title}
            </span>
          )}
          {preview.description && (
            <span className="line-clamp-2 text-xs text-muted-foreground">
              {preview.description}
            </span>
          )}
        </div>
      </a>
      {onRemove && (
        <Button
          type="button"
          variant="secondary"
          size="icon-xs"
          className="absolute top-1 right-1 opacity-80 hover:opacity-100"
          aria-label={t("chat.message.linkPreview.remove")}
          title={t("chat.message.linkPreview.remove")}
          onClick={onRemove}
        >
          <HugeiconsIcon icon={Cancel01Icon} />
        </Button>
      )}
    </div>
  );
}

function hostOf(url: string) {
  try {
    return new URL(url).hostname.replace(/^www\./, "");
  } catch {
    return url;
  }
}
