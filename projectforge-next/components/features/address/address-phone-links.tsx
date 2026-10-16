"use client";

import { useState, type MouseEvent, type SyntheticEvent } from "react";
import { createPortal } from "react-dom";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  Message01Icon,
  SmartPhone01Icon,
  TelephoneIcon,
} from "@hugeicons/core-free-icons";
import { PhoneCallDialog } from "@/components/features/phone-call/phone-call-dialog";
import { SendTextMessageDialog } from "@/components/features/send-text-message/send-text-message-dialog";
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
 * The phone numbers of an address, each a link that dials it — through the direct call of ProjectForge where one
 * is configured, else the device's own (`tel:`) — and the mobile numbers with a link to send a text message.
 * Shared by the list and the view.
 *
 * The direct call and the text message open in a dialog over the list or view, as in the React app; their links
 * still lead to the pages, for a new tab (ctrl/cmd click).
 *
 * Hovering a number shows it large across the top of the window, as the React app did: to be read off the screen
 * while dialling it on a desk phone.
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
  const [zoomed, setZoomed] = useState<{
    number: string;
    mobile: boolean;
  } | null>(null);
  const [dialog, setDialog] = useState<
    { kind: "call"; number: string } | { kind: "sms"; phoneType: string } | null
  >(null);
  const numbers = phoneNumbersOf(address);
  if (numbers.length === 0) return null;
  // A plain click opens the dialog; one with a modifier (new tab or window) follows the link to the page.
  const openInDialog =
    (open: () => void) => (event: MouseEvent<HTMLAnchorElement>) => {
      event.stopPropagation();
      if (
        event.button !== 0 ||
        event.metaKey ||
        event.ctrlKey ||
        event.shiftKey ||
        event.altKey
      ) {
        return;
      }
      event.preventDefault();
      // The zoom of the number would sit above the dialog until the pointer moves.
      setZoomed(null);
      open();
    };
  return (
    <>
      <ul className={cn("flex flex-col gap-0.5", className)}>
        {zoomed && <PhoneNumberZoom {...zoomed} />}
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
            <li
              key={entry.type}
              className="flex items-center gap-1.5"
              // A mouse only: a tap would show the zoom without a "leave" to hide it again.
              onPointerEnter={(event) =>
                event.pointerType === "mouse" &&
                setZoomed({ number: entry.number, mobile: entry.mobile })
              }
              onPointerLeave={() => setZoomed(null)}
            >
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
                  onClick={openInDialog(() =>
                    setDialog({ kind: "call", number: entry.number })
                  )}
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
                  onClick={openInDialog(() =>
                    setDialog({ kind: "sms", phoneType: entry.type })
                  )}
                >
                  <HugeiconsIcon icon={Message01Icon} size={14} />
                </Link>
              )}
            </li>
          );
        })}
      </ul>
      {dialog && address.id != null && (
        // The dialog is portalled, but its events bubble through the React tree: in a list row a click or a key
        // in it would reach the row (opening the view, moving the selection) — stopped here.
        <span className="contents" onClick={stop} onKeyDown={stop}>
          {dialog.kind === "call" ? (
            <PhoneCallDialog
              params={{
                addressId: address.id,
                number: dialog.number,
                callerPage,
              }}
              onClose={() => setDialog(null)}
            />
          ) : (
            <SendTextMessageDialog
              params={{ addressId: address.id, phoneType: dialog.phoneType }}
              onClose={() => setDialog(null)}
            />
          )}
        </span>
      )}
    </>
  );
}

function stop(event: SyntheticEvent) {
  event.stopPropagation();
}

/** The hovered number, large across the top of the window; it lets every click through. */
function PhoneNumberZoom({
  number,
  mobile,
}: {
  number: string;
  mobile: boolean;
}) {
  return createPortal(
    <div
      aria-hidden
      className="pointer-events-none fixed inset-x-0 top-0 z-[100] flex h-32 animate-in items-center justify-center gap-6 bg-background/80 text-6xl font-bold tabular-nums backdrop-blur-sm backdrop-saturate-150 duration-300 fade-in-0 zoom-in-50 print:hidden"
    >
      <HugeiconsIcon
        icon={mobile ? SmartPhone01Icon : TelephoneIcon}
        size={56}
      />
      {number}
    </div>,
    document.body
  );
}
