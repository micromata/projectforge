"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  Message01Icon,
  SmartPhone01Icon,
  TelephoneIcon,
} from "@hugeicons/core-free-icons";
import { cn } from "@/lib/utils";
import { phoneNumbersOf } from "./address-phones";
import type { AddressDetail } from "./types";

interface Props {
  address: AddressDetail;
  /** Whether a number may be called through ProjectForge (Sipgate configured); `tel:` otherwise. */
  phoneCallEnabled: boolean;
  /** Whether a text message may be sent to a mobile number (SMS configured). */
  smsEnabled: boolean;
  /** Where the phone call page leads back to, see `PhoneCallRest.backUrl`. */
  callerPage: "addressList" | "addressView";
  /** Shows the label of each number (business, private …) in front of it, as the view does. */
  withLabels?: boolean;
  className?: string;
}

/**
 * The phone numbers of an address, each a link that dials it — through the phone call page of ProjectForge
 * where one is configured, else the device's own (`tel:`) — and the mobile numbers with a link to send a text
 * message. Shared by the list and the view.
 */
export function AddressPhoneLinks({
  address,
  phoneCallEnabled,
  smsEnabled,
  callerPage,
  withLabels,
  className,
}: Props) {
  const t = useTranslations();
  const numbers = phoneNumbersOf(address);
  if (numbers.length === 0) return null;
  return (
    <ul className={cn("flex flex-col gap-0.5", className)}>
      {numbers.map((entry) => {
        const label = t(entry.labelKey);
        const callHref = phoneCallEnabled
          ? `/phoneCall?${new URLSearchParams({
              addressId: String(address.id),
              number: entry.number,
              callerPage,
            }).toString()}`
          : `tel:${entry.number}`;
        return (
          <li key={entry.type} className="flex items-center gap-1.5">
            <HugeiconsIcon
              icon={entry.mobile ? SmartPhone01Icon : TelephoneIcon}
              size={14}
              className="shrink-0 text-muted-foreground"
              aria-hidden
            />
            {withLabels && (
              <span className="text-muted-foreground">{label}:</span>
            )}
            {phoneCallEnabled ? (
              <Link
                href={callHref}
                className="hover:underline"
                aria-label={`${t("address.directCall.call")} ${entry.number} (${label})`}
                onClick={(event) => event.stopPropagation()}
              >
                {entry.number}
              </Link>
            ) : (
              <a
                href={callHref}
                className="hover:underline"
                onClick={(event) => event.stopPropagation()}
              >
                {entry.number}
              </a>
            )}
            {smsEnabled && entry.mobile && address.id != null && (
              <Link
                href={`/sendTextMessage?addressId=${address.id}&phoneType=${entry.type}`}
                className="text-muted-foreground hover:text-foreground"
                aria-label={`${t("address.sendSms.title")}: ${entry.number}`}
                onClick={(event) => event.stopPropagation()}
              >
                <HugeiconsIcon icon={Message01Icon} size={14} />
              </Link>
            )}
          </li>
        );
      })}
    </ul>
  );
}
