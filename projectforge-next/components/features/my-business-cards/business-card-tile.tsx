"use client";

import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { toast } from "@/lib/toast";
import {
  businessCardPassUrl,
  downloadBusinessCardVCard,
} from "@/lib/rs/my-business-cards";
import { BusinessCardContents } from "./business-card-contents";
import { BusinessCardQrPreview } from "./business-card-qr-preview";
import type { BusinessCardInfo } from "./types";

/** One card of the list: its name, its QR code (to show or to screenshot) and what can be done with it. */
export function BusinessCardTile({
  info,
  walletConfigured,
  onEdit,
}: {
  info: BusinessCardInfo;
  walletConfigured: boolean;
  onEdit: () => void;
}) {
  const t = useTranslations();
  const download = () =>
    downloadBusinessCardVCard(info.card.id!).catch((error) =>
      toast.error(error instanceof Error ? error.message : String(error))
    );
  return (
    <div className="flex w-72 flex-col gap-2 rounded-md border p-3">
      <div className="font-medium">{info.card.cardName}</div>
      <BusinessCardQrPreview preview={info.preview} />
      <BusinessCardContents
        card={info.card}
        withPhoto={info.preview.withPhoto}
      />
      <div className="flex flex-wrap gap-2">
        <Button variant="outline" size="sm" onClick={onEdit}>
          {t("edit")}
        </Button>
        <Button variant="outline" size="sm" onClick={() => void download()}>
          {t("businessCard.downloadVCard")}
        </Button>
        {walletConfigured && (
          <Button variant="outline" size="sm" asChild>
            <a href={businessCardPassUrl(info.card.id!)}>
              {t("businessCard.addToWallet")}
            </a>
          </Button>
        )}
      </div>
    </div>
  );
}
