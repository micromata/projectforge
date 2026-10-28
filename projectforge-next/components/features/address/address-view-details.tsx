"use client";

import { leafKeyOf } from "@/lib/leaf-key";
import { useTranslations } from "next-intl";
import { SectionCard } from "@/components/shared/section-card";
import { useFormatContext } from "@/hooks/use-format";
import { formatDate } from "@/lib/format";
import { cn } from "@/lib/utils";
import { addressImageUrl, type AddressViewData } from "@/lib/rs/address";
import { AddressFavoriteToggle } from "./address-favorite-toggle";
import { AddressPhoneLinks } from "./address-phone-links";
import { websiteHref } from "./address-links";
import { AddressPostalBlock } from "./address-postal-block";
import type { AddressDetail } from "./types";

interface Props {
  view: AddressViewData<AddressDetail>;
  onFavoriteChanged: () => void;
  className?: string;
}

/**
 * The body of the address view: the image and the favorite star, the phone numbers, the contact data in a
 * table, the three addresses and the comment — the former view's layout. Only what the address has is shown.
 */
export function AddressViewDetails({
  view,
  onFavoriteChanged,
  className,
}: Props) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const { address } = view;
  const rows: Row[] = [
    { label: t("organization"), value: address.organization },
    { label: t("address.division"), value: address.division },
    { label: t("address.positionText"), value: address.positionText },
    {
      label: t("address.birthday"),
      value: address.birthday ? formatDate(address.birthday, ctx) : null,
    },
    { label: t("address.website"), value: address.website, kind: "web" },
    {
      label: `${t("email")} (${t("address.business")})`,
      value: address.email,
      kind: "mail",
    },
    {
      label: t("address.privateEmail"),
      value: address.privateEmail,
      kind: "mail",
    },
    {
      label: t(leafKeyOf("address.addressStatus", t.has)),
      value: address.addressStatusAsString,
    },
    {
      label: t(leafKeyOf("address.contactStatus", t.has)),
      value: address.contactStatusAsString,
    },
  ];

  return (
    <div className={cn("flex flex-col gap-4 px-4 py-3", className)}>
      <SectionCard className="flex flex-col gap-4 sm:flex-row">
        {address.hasImage && address.id != null && (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={`${addressImageUrl(address.id)}?v=${encodeURIComponent(address.lastUpdate ?? "")}`}
            alt={t("address.image")}
            className="max-h-56 max-w-56 self-start rounded-md border object-contain"
          />
        )}
        <div className="flex flex-col gap-3">
          <div className="flex items-center gap-1 text-sm print:hidden">
            <AddressFavoriteToggle
              addressId={address.id!}
              favorite={address.isFavoriteCard === true}
              onChanged={onFavoriteChanged}
            />
            {t(leafKeyOf("favorite", t.has))}
          </div>
          <AddressPhoneLinks
            address={address}
            phoneCallEnabled={view.phoneCallEnabled}
            smsEnabled={view.smsEnabled}
            callerPage="addressView"
            withLabels
            className="text-sm"
          />
        </div>
      </SectionCard>
      <div className="grid gap-4 lg:grid-cols-2">
        <SectionCard>
          <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1 text-sm">
            {rows
              .filter((row) => row.value?.trim())
              .map((row) => (
                <div key={row.label} className="contents">
                  <dt className="text-muted-foreground">{row.label}</dt>
                  <dd className="break-words">
                    <RowValue row={row} />
                  </dd>
                </div>
              ))}
          </dl>
        </SectionCard>
        <SectionCard className="flex flex-col gap-4 text-sm">
          <AddressPostalBlock address={address} block="business" />
          <AddressPostalBlock address={address} block="private" />
          <AddressPostalBlock address={address} block="postal" />
        </SectionCard>
      </div>
      {address.comment?.trim() && (
        <SectionCard className="text-sm">
          <p className="mb-1 font-semibold">{t("comment")}</p>
          <p className="whitespace-pre-wrap">{address.comment}</p>
        </SectionCard>
      )}
    </div>
  );
}

interface Row {
  label: string;
  value: string | null | undefined;
  /** A website or an e-mail address is a link. */
  kind?: "web" | "mail";
}

function RowValue({ row }: { row: Row }) {
  const value = row.value!;
  if (row.kind === "web") {
    return (
      <a
        href={websiteHref(value)}
        target="_blank"
        rel="noreferrer"
        className="underline"
      >
        {value}
      </a>
    );
  }
  if (row.kind === "mail") {
    return (
      <a href={`mailto:${value}`} className="underline">
        {value}
      </a>
    );
  }
  return value;
}
