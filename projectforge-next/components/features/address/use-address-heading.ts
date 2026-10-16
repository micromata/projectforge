"use client";

import { useTranslations } from "next-intl";
import { ADDRESS_METADATA } from "@/lib/metadata/address.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import type { AddressDetail } from "./types";

const m = fromMetadata(ADDRESS_METADATA);

/** The forms of address that are part of the name ("Mr", "Ms"); a company or an unknown one is not. */
const SALUTATIONS = ["MISTER", "MISS"];

/**
 * The heading of an address: the form of address, the title and the names (`AddressDO.fullNameWithTitleAndForm`),
 * then the organization. Only the organization, if the address names nobody.
 */
export function useAddressHeading(address: AddressDetail | undefined): string {
  const t = useTranslations();
  if (!address) return "";
  const form =
    address.form && SALUTATIONS.includes(address.form)
      ? m.enumOptions("form", t).find((option) => option.value === address.form)
          ?.label
      : undefined;
  const name = [form, address.title, address.firstName, address.name]
    .filter((part) => part && part.trim())
    .join(" ");
  if (!name) return address.organization ?? "";
  return address.organization ? `${name} – ${address.organization}` : name;
}
