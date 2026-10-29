"use client";

import { useTranslations } from "next-intl";
import { CheckboxField } from "@/components/shared/form/checkbox-field";
import { leafKeyOf } from "@/lib/leaf-key";

/**
 * Whether the photo of the source address is published (via the gateway) and linked in the vCard: only for the
 * user's own address and only with a gateway; otherwise the box is locked and a note says why. A ticked box
 * stays changeable, so it can always be unticked.
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
  const locked = !gatewayConfigured || !photoAllowed;
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
      {gatewayConfigured && hasAddress && !photoAllowed && (
        <p className="text-xs text-muted-foreground">
          {t("businessCard.includePhoto.notAllowed")}
        </p>
      )}
    </div>
  );
}
