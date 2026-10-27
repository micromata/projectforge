"use client";

import { useEffect, useRef } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import {
  playAttentionSound,
  unlockAttentionSound,
} from "@/lib/attention-sound";
import { setFaviconBadge } from "@/lib/favicon-badge";
import { claimOfferSignal } from "@/lib/offer-signals";
import { addRingLogEntry } from "@/lib/ring-log";
import { useChatSecretOffers } from "@/hooks/use-chat-secret-offers";
import type { ChatSecretSession } from "@/lib/rs/chat-secret";
import { useBlinkingTitle } from "./use-blinking-title";

/**
 * Draws the user's attention to a confidential conversation offered to them (PF-CHAT CONF-10), on whatever page
 * is open, also in a background tab: the offer expires within minutes. Per new offer once a sound and a browser
 * notification (if allowed); while any is open, a blinking tab title and a dot on the favicon. Names only the
 * initiator. The offers are reloaded by the user's event stream (useUserEvents), not polled.
 *
 * Mounted once for the authenticated area. The offer itself is answered in the chat (SecretOffers).
 */
export function SecretOfferAlerts() {
  const t = useTranslations();
  const router = useRouter();
  const offers = useChatSecretOffers();
  /** The offers signalled already in this tab: a refetch mustn't ring again. */
  const signalled = useRef(new Set<string>());
  const notifications = useRef(new Map<string, Notification>());

  useEffect(() => unlockAttentionSound(), []);

  useEffect(() => {
    const ids = new Set(offers.map((it) => it.id));
    // Fresh in this tab, and not signalled by this or another tab before (a reload or a second tab stays quiet).
    const fresh = offers.filter((it) => {
      if (signalled.current.has(it.id)) return false;
      signalled.current.add(it.id);
      return claimOfferSignal(it.id);
    });
    fresh.forEach((offer) => {
      const notification = notify(
        t("chat.notification.secret.title", {
          arg0: offer.initiator.displayName,
        }),
        offer,
        () => router.push(`/chat/${offer.conversationId}`)
      );
      if (notification) notifications.current.set(offer.id, notification);
    });
    if (fresh.length > 0) {
      const names = fresh.map((it) => it.initiator.displayName).join(", ");
      void playAttentionSound().then((outcome) =>
        addRingLogEntry({ outcome, reason: "secretOffer", arg: names })
      );
    }
    // Answered, withdrawn or expired: its system notification goes too.
    notifications.current.forEach((notification, id) => {
      if (ids.has(id)) return;
      notification.close();
      notifications.current.delete(id);
    });
    void setFaviconBadge(offers.length > 0);
  }, [offers, router, t]);

  useEffect(
    () => () => {
      notifications.current.forEach((it) => it.close());
      void setFaviconBadge(false);
    },
    []
  );

  const first = offers[0];
  useBlinkingTitle(
    first
      ? `🔒 ${t("chat.secret.alert.title", { arg0: first.initiator.displayName })}`
      : null
  );
  return null;
}

/** A notification of the system, if the user allowed them for this site (asked in SecretAlertSettings). */
function notify(
  title: string,
  offer: ChatSecretSession,
  onClick: () => void
): Notification | null {
  if (typeof Notification === "undefined") return null;
  if (Notification.permission !== "granted") return null;
  try {
    const notification = new Notification(title, {
      tag: `pf-chat-secret-${offer.id}`,
      icon: "/favicon.ico",
      // Stays until answered or closed (the offer expires within minutes), and rings by the system's own sound.
      requireInteraction: true,
      silent: false,
    });
    notification.onclick = () => {
      window.focus();
      onClick();
      notification.close();
    };
    return notification;
  } catch {
    // Some mobile browsers allow notifications only from a service worker.
    return null;
  }
}
