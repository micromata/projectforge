"use client";

import { useTranslations } from "next-intl";
import { CheckboxField } from "@/components/shared/form/checkbox-field";
import { leafKeyOf } from "@/lib/leaf-key";

/**
 * Whether the photo of the source address is used: embedded in the Wallet pass and, with a gateway, published there
 * and linked in the vCard. Only for the user's own address; otherwise the box is locked and a note says why. A
 * ticked box stays changeable, so it can always be unticked.
 */
export function BusinessCardPhotoOption({
  includePhoto,
  hasAddress,
  photoAllowed,
  gatewayConfigured,
}: {
  includePhoto: boolean;
  hasAddress: boolean;
  photoAllowed: boolean;
  gatewayConfigured: boolean;
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
      {!gatewayConfigured && (
        <p className="text-xs text-muted-foreground">
          {t("businessCard.includePhoto.noGateway")}
        </p>
      )}
      {hasAddress && !photoAllowed && (
        <p className="text-xs text-muted-foreground">
          {t("businessCard.includePhoto.notAllowed")}
        </p>
      )}
    </div>
  );
}
