"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Download01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from "@/components/ui/dialog";
import { chatFileUrl } from "@/lib/rs/chat";
import type { ChatFile } from "@/lib/rs/chat-types";

/** An image of a message full size, with download. */
export function ImageLightbox({
  conversationId,
  file,
  onClose,
}: {
  conversationId: number;
  file: ChatFile | null;
  onClose: () => void;
}) {
  const t = useTranslations();
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
            <div className="flex justify-end">
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
