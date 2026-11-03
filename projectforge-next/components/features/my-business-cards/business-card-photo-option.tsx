"use client";

import { useTranslations } from "next-intl";
import { CheckboxField } from "@/components/shared/form/checkbox-field";
import { leafKeyOf } from "@/lib/leaf-key";
import { BusinessCardRenewPhotoLinkButton } from "./business-card-renew-photo-link-button";

/**
 * Whether the photo of the source address is used: embedded in the Wallet pass and, with a gateway, published there
 * and linked in the vCard. Only for the user's own address; otherwise the box is locked and a note says why. A
 * ticked box stays changeable, so it can always be unticked. A stored card with an own address can renew the photo
 * link (revoke the public url, also when the photo is switched off: urls may still be out there).
 */
export function BusinessCardPhotoOption({
  includePhoto,
  hasAddress,
  photoAllowed,
  photoUrlAvailable,
  cardId,
}: {
  cardId: string | null;
  includePhoto: boolean;
  hasAddress: boolean;
  photoAllowed: boolean;
  photoUrlAvailable: boolean;
}) {
  const t = useTranslations();
  const locked = !photoAllowed;
  return (
    <div className="flex flex-col gap-1">
      <CheckboxField
        name="includePhoto"
        label={t(leafKeyOf("businessCard.includePhoto", t.has))}
        hint={t("businessCard.includePhoto.tooltip")}
        disabled={locked && !includePhoto}
      />
      {!photoUrlAvailable && (
        <p className="text-xs text-muted-foreground">
          {t("businessCard.includePhoto.noPhotoUrl")}
        </p>
      )}
      {cardId && hasAddress && photoAllowed && photoUrlAvailable && (
        <>
          <p className="text-xs text-muted-foreground">
            {t("businessCard.photoLink.hint")}
          </p>
          <BusinessCardRenewPhotoLinkButton cardId={cardId} />
        </>
      )}
      {hasAddress && !photoAllowed && (
        <p className="text-xs text-muted-foreground">
          {t("businessCard.includePhoto.notAllowed")}
        </p>
      )}
    </div>
  );
}
