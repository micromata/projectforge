"use client";

import { useTranslations } from "next-intl";
import { leafKeyOf } from "@/lib/leaf-key";
import { DENSE_VCARD_LENGTH, type BusinessCardPreview } from "./types";

/** The QR code of a card, with a warning when the vCard makes it dense. */
export function BusinessCardQrPreview({
  preview,
}: {
  preview: BusinessCardPreview | undefined;
}) {
  const t = useTranslations();
  if (!preview) return null;
  return (
    <div className="flex max-w-xs flex-col gap-2">
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img
        src={preview.qrCodeUrl}
        alt={t(leafKeyOf("businessCard.qrCode", t.has))}
        className="aspect-square w-full rounded-md border bg-white p-2"
      />
      {preview.vCardLength > DENSE_VCARD_LENGTH && (
        <p className="text-xs text-muted-foreground">
          {t("businessCard.qrCode.dense")}
        </p>
      )}
    </div>
  );
}
