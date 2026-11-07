"use client";

import { useTranslations } from "next-intl";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
import { InputField } from "@/components/shared/form/input-field";
import { BusinessCardAddressField } from "./business-card-address-field";
import { BusinessCardContents } from "./business-card-contents";
import { BusinessCardPhotoOption } from "./business-card-photo-option";
import { BusinessCardQrPreview } from "./business-card-qr-preview";
import type { BusinessCard, BusinessCardPreview } from "./types";

/** The first section of the editor: name, source address and photo of the card beside its live QR code. */
export function BusinessCardMainSection({
  address,
  suggestionAddressId,
  onPickAddress,
  includePhoto,
  photoUrlAvailable,
  cardId,
  preview,
  card,
}: {
  cardId: string | null;
  address: EntityRef | null;
  suggestionAddressId: number | null;
  onPickAddress: (ref: EntityRef | null) => void;
  includePhoto: boolean;
  photoUrlAvailable: boolean;
  preview: BusinessCardPreview | undefined;
  /** The card as it would be saved, to list what it contains. */
  card: BusinessCard;
}) {
  const t = useTranslations();
  return (
    <div className="flex flex-col gap-4 lg:flex-row">
      <div className="flex flex-1 flex-col gap-4">
        <InputField name="cardName" label={t("businessCard.cardName")} />
        <BusinessCardAddressField
          value={address}
          suggestionAddressId={suggestionAddressId}
          onPick={onPickAddress}
        />
        <BusinessCardPhotoOption
          includePhoto={includePhoto}
          hasAddress={address != null}
          photoAllowed={preview?.photoAllowed === true}
          photoUrlAvailable={photoUrlAvailable}
          cardId={cardId}
        />
      </div>
      <div className="flex max-w-xs flex-col gap-3">
        <BusinessCardQrPreview preview={preview} />
        <BusinessCardContents
          card={card}
          withPhoto={preview?.withPhoto === true}
        />
      </div>
    </div>
  );
}
