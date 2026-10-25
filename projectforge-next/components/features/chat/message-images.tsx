"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { chatImageUrl } from "@/lib/rs/chat";
import { ImageLightbox } from "./image-lightbox";

/** The images of a message as thumbnails; a click opens one full size (ATT-06). */
export function MessageImages({
  conversationId,
  imageIds,
}: {
  conversationId: number;
  imageIds: string[];
}) {
  const t = useTranslations();
  const [open, setOpen] = useState<string | null>(null);
  if (imageIds.length === 0) return null;
  return (
    <>
      <ul className="mt-1 flex flex-wrap gap-2">
        {imageIds.map((fileId, i) => (
          <li key={fileId}>
            <button
              type="button"
              className="block overflow-hidden rounded-md border hover:opacity-90"
              aria-label={`${t("chat.image.download")} ${i + 1}`}
              onClick={() => setOpen(fileId)}
            >
              {/* eslint-disable-next-line @next/next/no-img-element -- served by Spring with the session, no optimizer */}
              <img
                src={chatImageUrl(conversationId, fileId)}
                alt=""
                loading="lazy"
                className="max-h-40 max-w-60 object-cover"
              />
            </button>
          </li>
        ))}
      </ul>
      <ImageLightbox
        conversationId={conversationId}
        fileId={open}
        onClose={() => setOpen(null)}
      />
    </>
  );
}
