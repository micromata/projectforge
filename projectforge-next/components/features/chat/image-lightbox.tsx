"use client";

import { useEffect } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Download01Icon, FolderOpenIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from "@/components/ui/dialog";
import { chatFileUrl } from "@/lib/rs/chat";
import type { ChatFile } from "@/lib/rs/chat-types";
import { ImagePager } from "./image-pager";

/**
 * An image of a message full size, with download and a link to its data transfer area.
 * With several images, the buttons and the arrow keys page through them.
 */
export function ImageLightbox({
  conversationId,
  files,
  index,
  onIndexChange,
  onClose,
}: {
  conversationId: number;
  files: ChatFile[];
  index: number | null;
  onIndexChange: (index: number) => void;
  onClose: () => void;
}) {
  const t = useTranslations();
  const file = index != null ? (files[index] ?? null) : null;
  const paging = index != null && files.length > 1;
  const count = files.length;
  // On the window, not the dialog: paging to the first or last image disables the focused pager button, which
  // drops the focus out of the dialog.
  useEffect(() => {
    if (index == null || count < 2) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "ArrowLeft" && index > 0) onIndexChange(index - 1);
      if (event.key === "ArrowRight" && index < count - 1)
        onIndexChange(index + 1);
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [index, count, onIndexChange]);
  return (
    <Dialog open={file != null} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[95dvh] max-w-[95vw] gap-2 p-2 sm:max-w-[90vw]">
        <DialogTitle className="sr-only">{file?.name}</DialogTitle>
        <DialogDescription className="sr-only">
          {t("chat.file.download")}
        </DialogDescription>
        {file && (
          <>
            {/* eslint-disable-next-line @next/next/no-img-element -- served by Spring with the session */}
            <img
              src={chatFileUrl(conversationId, file.fileId)}
              alt=""
              className="mx-auto max-h-[85dvh] max-w-full object-contain"
            />
            <div className="flex items-center justify-end gap-2">
              {paging && (
                <div className="mr-auto">
                  <ImagePager
                    index={index}
                    count={files.length}
                    onChange={onIndexChange}
                  />
                </div>
              )}
              <Button asChild variant="outline" size="sm">
                <Link href={`/datatransfer/${file.areaId}`} onClick={onClose}>
                  <HugeiconsIcon
                    icon={FolderOpenIcon}
                    data-icon="inline-start"
                  />
                  {t("chat.file.openArea")}
                </Link>
              </Button>
              <Button asChild variant="outline" size="sm">
                <a
                  href={chatFileUrl(conversationId, file.fileId, true)}
                  download={file.name}
                >
                  <HugeiconsIcon
                    icon={Download01Icon}
                    data-icon="inline-start"
                  />
                  {t("chat.file.download")}
                </a>
              </Button>
            </div>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}
