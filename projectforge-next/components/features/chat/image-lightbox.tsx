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
import { chatImageUrl } from "@/lib/rs/chat";

/** An image of a message full size, with download. */
export function ImageLightbox({
  conversationId,
  fileId,
  onClose,
}: {
  conversationId: number;
  fileId: string | null;
  onClose: () => void;
}) {
  const t = useTranslations();
  return (
    <Dialog open={fileId != null} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[95dvh] max-w-[95vw] gap-2 p-2 sm:max-w-[90vw]">
        <DialogTitle className="sr-only">
          {t("chat.image.download")}
        </DialogTitle>
        <DialogDescription className="sr-only">{fileId}</DialogDescription>
        {fileId && (
          <>
            {/* eslint-disable-next-line @next/next/no-img-element -- served by Spring with the session */}
            <img
              src={chatImageUrl(conversationId, fileId)}
              alt=""
              className="mx-auto max-h-[85dvh] max-w-full object-contain"
            />
            <div className="flex justify-end">
              <Button asChild variant="outline" size="sm">
                <a href={chatImageUrl(conversationId, fileId, true)} download>
                  <HugeiconsIcon
                    icon={Download01Icon}
                    data-icon="inline-start"
                  />
                  {t("chat.image.download")}
                </a>
              </Button>
            </div>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}
