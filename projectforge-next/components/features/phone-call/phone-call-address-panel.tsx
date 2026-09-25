"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { SmartPhone01Icon, TelephoneIcon } from "@hugeicons/core-free-icons";
import { SectionCard } from "@/components/shared/section-card";
import { Button } from "@/components/ui/button";
import type { AddressInfo, AddressPhoneNumber } from "./types";

type Translate = ReturnType<typeof useTranslations>;

/**
 * Literal `t(...)` calls per type on purpose: the i18n generator scans for `t("literal")` and would miss a
 * key looked up through a map, so `address.phoneType.*` would never reach the catalog.
 */
function phoneTypeLabel(
  t: Translate,
  phoneType: AddressPhoneNumber["phoneType"]
): string {
  switch (phoneType) {
    case "BUSINESS":
      return t("address.phoneType.business");
    case "MOBILE":
      return t("address.phoneType.mobile");
    case "PRIVATE":
      return t("address.phoneType.private");
    case "PRIVATE_MOBILE":
      return t("address.phoneType.privateMobile");
  }
}

function isMobile(phoneType: AddressPhoneNumber["phoneType"]): boolean {
  return phoneType === "MOBILE" || phoneType === "PRIVATE_MOBILE";
}

/**
 * The resolved address beside the form (deep-link `addressId`), with its numbers as one-click dial buttons —
 * the convenience of the Wicket page's right column. A click seeds the number field and asks for confirmation
 * (see [PhoneCallForm]); the number itself is dialed the same way the typed one is.
 */
export function PhoneCallAddressPanel({
  address,
  onPick,
}: {
  address: AddressInfo;
  onPick: (number: string) => void;
}) {
  const t = useTranslations();
  return (
    <SectionCard className="w-full md:w-96 md:shrink-0">
      <p className="mb-3 font-medium">{address.fullName}</p>
      <ul className="flex flex-col gap-1">
        {address.numbers.map((entry) => (
          <li key={`${entry.phoneType}-${entry.number}`}>
            <Button
              type="button"
              variant="ghost"
              className="h-auto w-full justify-start gap-2 px-2 py-1.5 text-left"
              onClick={() => onPick(entry.number)}
              aria-label={`${t("address.directCall.call")} ${entry.number}`}
            >
              <HugeiconsIcon
                className="shrink-0"
                icon={
                  isMobile(entry.phoneType) ? SmartPhone01Icon : TelephoneIcon
                }
                size={16}
              />
              <span className="flex min-w-0 flex-col">
                <span className="break-words">{entry.number}</span>
                <span className="text-xs text-muted-foreground">
                  {phoneTypeLabel(t, entry.phoneType)}
                </span>
              </span>
            </Button>
          </li>
        ))}
      </ul>
    </SectionCard>
  );
}
