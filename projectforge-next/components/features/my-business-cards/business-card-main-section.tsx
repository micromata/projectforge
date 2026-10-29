"use client";

import { useTranslations } from "next-intl";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
import { InputField } from "@/components/shared/form/input-field";
import { BusinessCardAddressField } from "./business-card-address-field";
import { BusinessCardPhotoOption } from "./business-card-photo-option";
import { BusinessCardQrPreview } from "./business-card-qr-preview";
import type { BusinessCardPreview } from "./types";

/** The first section of the editor: name, source address and photo of the card beside its live QR code. */
export function BusinessCardMainSection({
  address,
  suggestionAddressId,
  onPickAddress,
  includePhoto,
  gatewayConfigured,
  preview,
}: {
  address: EntityRef | null;
  suggestionAddressId: number | null;
  onPickAddress: (ref: EntityRef | null) => void;
  includePhoto: boolean;
  gatewayConfigured: boolean;
  preview: BusinessCardPreview | undefined;
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
          gatewayConfigured={gatewayConfigured}
        />
      </div>
      <BusinessCardQrPreview preview={preview} />
    </div>
  );
}
