"use client";

import { useTranslations } from "next-intl";
import type { AddressDetail } from "./types";

type Block = "business" | "private" | "postal";

/**
 * One of the three addresses of an address as it is written on an envelope: the business one under the
 * organization and its division, the other two under their heading. Nothing if the block is empty.
 */
export function AddressPostalBlock({
  address,
  block,
}: {
  address: AddressDetail;
  block: Block;
}) {
  const t = useTranslations();
  const lines = linesOf(address, block);
  if (lines.length === 0) return null;
  const heading =
    block === "private"
      ? t("address.heading.privateAddress")
      : block === "postal"
        ? t("address.heading.postalAddress")
        : null;
  const head =
    block === "business"
      ? [address.organization, address.division].filter((line) => line?.trim())
      : [];
  return (
    <address className="not-italic">
      {heading && <p className="mb-0.5 font-semibold">{heading}</p>}
      {[...head, ...lines].map((line, index) => (
        <p key={index}>{line}</p>
      ))}
    </address>
  );
}

function linesOf(address: AddressDetail, block: Block): string[] {
  const field = (name: string) => {
    const key =
      block === "business"
        ? name
        : `${block}${name.charAt(0).toUpperCase()}${name.slice(1)}`;
    return (
      (
        (address as unknown as Record<string, unknown>)[key] as
          | string
          | null
          | undefined
      )?.trim() ?? ""
    );
  };
  const place = [field("zipCode"), field("city")].filter(Boolean).join(" ");
  const region = [field("state"), field("country")].filter(Boolean).join(", ");
  return [field("addressText"), field("addressText2"), place, region].filter(
    Boolean
  );
}
