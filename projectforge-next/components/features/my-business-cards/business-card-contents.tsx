"use client";

import type { ReactNode } from "react";
import { useState } from "react";
import { useTranslations } from "next-intl";
import { useFormatContext } from "@/hooks/use-format";
import { formatDate } from "@/lib/format";
import { leafKeyOf } from "@/lib/leaf-key";
import { businessCardPhotoUrl } from "@/lib/rs/my-business-cards";
import { BUSINESS_CARD_FIELDS, type BusinessCard } from "./types";

/** Small control list below the QR code: every field (and the photo) that the card really contains. */
export function BusinessCardContents({
  card,
  withPhoto,
}: {
  card: BusinessCard;
  withPhoto: boolean;
}) {
  const t = useTranslations();
  const formatContext = useFormatContext();
  const rows: { key: string; label: string; value: ReactNode }[] =
    BUSINESS_CARD_FIELDS.flatMap(({ field, labelKey }) => {
      const value = card[field];
      if (!value) return [];
      return [
        {
          key: field,
          label: t(labelKey),
          value:
            field === "birthday" ? formatDate(value, formatContext) : value,
        },
      ];
    });
  if (withPhoto) {
    const label = t(leafKeyOf("businessCard.includePhoto", t.has));
    rows.push({
      key: "photo",
      label,
      value: card.addressId ? (
        <BusinessCardPhotoThumb addressId={card.addressId} label={label} />
      ) : (
        "✓"
      ),
    });
  }
  if (rows.length === 0) return null;
  return (
    <dl className="grid grid-cols-[auto_minmax(0,1fr)] gap-x-2 text-xs text-muted-foreground">
      {rows.map(({ key, label, value }) => (
        <div key={key} className="contents">
          <dt>{label}</dt>
          <dd className="break-all text-foreground">{value}</dd>
        </div>
      ))}
    </dl>
  );
}

/** The small photo of the card; when it can't be loaded, the box says so instead of showing a broken image. */
function BusinessCardPhotoThumb({
  addressId,
  label,
}: {
  addressId: number;
  label: string;
}) {
  const t = useTranslations();
  const [failed, setFailed] = useState(false);
  if (failed) {
    return (
      <span className="text-destructive">
        {t("businessCard.photoUnavailable")}
      </span>
    );
  }
  return (
    // eslint-disable-next-line @next/next/no-img-element
    <img
      src={businessCardPhotoUrl(addressId)}
      alt={label}
      onError={() => setFailed(true)}
      className="size-12 rounded-md border object-cover"
    />
  );
}
