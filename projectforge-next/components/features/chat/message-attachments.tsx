"use client";

import { useState } from "react";
import { chatFileUrl } from "@/lib/rs/chat";
import type { ChatFile } from "@/lib/rs/chat-types";
import { ImageLightbox } from "./image-lightbox";
import { MessageFilesList } from "./message-files-list";

/**
 * The files of a message (ATT-06): images as thumbnails, a click opens one full size; other files as downloads,
 * each with a link to its data transfer area.
 * The images stand side by side in one row (scrolling sideways when there are many); the lightbox pages through them.
 */
export function MessageAttachments({
  conversationId,
  files,
}: {
  conversationId: number;
  files: ChatFile[];
}) {
  const [open, setOpen] = useState<number | null>(null);
  if (files.length === 0) return null;
  const images = files.filter((file) => file.image && file.available);
  const others = files.filter((file) => !file.image || !file.available);
  return (
    <>
      {images.length > 0 && (
        <ul className="mt-1 flex max-w-full gap-2 overflow-x-auto pb-1">
          {images.map((file, index) => (
            <li key={file.fileId} className="shrink-0">
              <button
                type="button"
                className="block overflow-hidden rounded-md border hover:opacity-90"
                aria-label={file.name}
                onClick={() => setOpen(index)}
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
      <MessageFilesList conversationId={conversationId} files={others} />
      <ImageLightbox
        conversationId={conversationId}
        files={images}
        index={open}
        onIndexChange={setOpen}
        onClose={() => setOpen(null)}
      />
    </>
  );
}
