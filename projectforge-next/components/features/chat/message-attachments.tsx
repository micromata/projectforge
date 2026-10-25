"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { File01Icon } from "@hugeicons/core-free-icons";
import { chatFileUrl } from "@/lib/rs/chat";
import type { ChatFile } from "@/lib/rs/chat-types";
import { cn } from "@/lib/utils";
import { ImageLightbox } from "./image-lightbox";

/**
 * The files of a message (ATT-06): images as thumbnails, a click opens one full size; other files as downloads.
 * A file expired or deleted in the data transfer stays listed as no longer available.
 */
export function MessageAttachments({
  conversationId,
  files,
}: {
  conversationId: number;
  files: ChatFile[];
}) {
  const t = useTranslations();
  const [open, setOpen] = useState<ChatFile | null>(null);
  if (files.length === 0) return null;
  const images = files.filter((file) => file.image && file.available);
  const others = files.filter((file) => !file.image || !file.available);
  return (
    <>
      {images.length > 0 && (
        <ul className="mt-1 flex flex-wrap gap-2">
          {images.map((file) => (
            <li key={file.fileId}>
              <button
                type="button"
                className="block overflow-hidden rounded-md border hover:opacity-90"
                aria-label={file.name}
                onClick={() => setOpen(file)}
              >
                {/* eslint-disable-next-line @next/next/no-img-element -- served by Spring with the session, no optimizer */}
                <img
                  src={chatFileUrl(conversationId, file.fileId)}
                  alt=""
                  loading="lazy"
                  className="max-h-40 max-w-60 object-cover"
                />
              </button>
            </li>
          ))}
        </ul>
      )}
      {others.length > 0 && (
        <ul className="mt-1 flex flex-wrap gap-2">
          {others.map((file) => {
            const content = (
              <>
                <HugeiconsIcon icon={File01Icon} className="size-4 shrink-0" />
                <span className="min-w-0 truncate">{file.name}</span>
                <span className="shrink-0 text-muted-foreground">
                  {file.available
                    ? file.sizeFormatted
                    : t("chat.file.notAvailable")}
                </span>
              </>
            );
            const className = cn(
              "flex max-w-80 items-center gap-1.5 rounded-md border px-2 py-1.5 text-xs",
              file.available
                ? "hover:bg-muted"
                : "text-muted-foreground opacity-70"
            );
            return (
              <li key={file.fileId}>
                {file.available ? (
                  <a
                    href={chatFileUrl(conversationId, file.fileId, true)}
                    download={file.name}
                    className={className}
                    aria-label={`${t("chat.file.download")}: ${file.name}`}
                  >
                    {content}
                  </a>
                ) : (
                  <span className={className}>{content}</span>
                )}
              </li>
            );
          })}
        </ul>
      )}
      <ImageLightbox
        conversationId={conversationId}
        file={open}
        onClose={() => setOpen(null)}
      />
    </>
  );
}
