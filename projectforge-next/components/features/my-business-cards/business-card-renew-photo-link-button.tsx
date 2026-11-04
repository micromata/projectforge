"use client";

import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { Button } from "@/components/ui/button";
import {
  MY_BUSINESS_CARDS_QUERY_KEY,
  renewBusinessCardPhotoLink,
} from "@/lib/rs/my-business-cards";
import { toast } from "@/lib/toast";

/**
 * Revokes the public photo url of the card's address after asking: QR codes and vCards handed out before show no
 * photo any more, the preview (QR code) of the card gets the new url.
 */
export function BusinessCardRenewPhotoLinkButton({
  cardId,
}: {
  cardId: string;
}) {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const renew = async () => {
    setOpen(false);
    try {
      await renewBusinessCardPhotoLink(cardId);
      await queryClient.invalidateQueries({
        queryKey: MY_BUSINESS_CARDS_QUERY_KEY,
      });
      toast.success(t("businessCard.photoLink.renewed"));
    } catch (error) {
      toast.error(error instanceof Error ? error.message : String(error));
    }
  };
  return (
    <>
      <Button
        type="button"
        variant="outline"
        size="sm"
        className="self-start"
        onClick={() => setOpen(true)}
      >
        {t("businessCard.photoLink.renew")}
      </Button>
      <ConfirmDialog
        open={open}
        onOpenChange={setOpen}
        title={t("businessCard.photoLink.renew")}
        description={t("businessCard.photoLink.confirmRenew")}
        confirmLabel={t("businessCard.photoLink.renew")}
        destructive
        onConfirm={() => void renew()}
      />
    </>
  );
}
