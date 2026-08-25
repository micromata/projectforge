"use client";

import { useCallback, useEffect, useMemo, useSyncExternalStore } from "react";
import { NextIntlClientProvider } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { fetchCustomerI18nOverrides } from "@/lib/rs/i18n";
import {
  applyCustomerOverrides,
  DEFAULT_LOCALE,
  DEFAULT_TIME_ZONE,
  LOCALE_COOKIE,
  MESSAGES,
  normalizeLocale,
  type Locale,
} from "./config";

function readCookieLocale(): Locale | undefined {
  const match = document.cookie
    .split("; ")
    .find((entry) => entry.startsWith(`${LOCALE_COOKIE}=`));
  return normalizeLocale(match?.slice(LOCALE_COOKIE.length + 1));
}

// Locale lives in a tiny external store rather than component state: the static
// export prerenders with DEFAULT_LOCALE, and useSyncExternalStore lets the
// client adopt the real locale after hydration without a mismatch.
let currentLocale: Locale | null = null;
const listeners = new Set<() => void>();

function getSnapshot(): Locale {
  if (currentLocale === null) {
    currentLocale =
      readCookieLocale() ??
      normalizeLocale(navigator.language) ??
      DEFAULT_LOCALE;
  }
  return currentLocale;
}

function getServerSnapshot(): Locale {
  return DEFAULT_LOCALE;
}

function subscribe(onChange: () => void) {
  listeners.add(onChange);
  return () => listeners.delete(onChange);
}

/**
 * Resolves the UI locale on the client, because the static export has no server
 * to do it per request. Order: cookie (explicit choice or last known backend
 * locale) → browser language → default.
 */
export function useLocale() {
  const locale = useSyncExternalStore(
    subscribe,
    getSnapshot,
    getServerSnapshot
  );

  const setLocale = useCallback((next: Locale) => {
    if (currentLocale === next) return;
    currentLocale = next;
    // Session cookie, path-scoped to the app; the backend stays the source of truth.
    document.cookie = `${LOCALE_COOKIE}=${next}; path=/; SameSite=Lax`;
    listeners.forEach((listener) => listener());
  }, []);

  return { locale, setLocale };
}

export function LocaleProvider({ children }: { children: React.ReactNode }) {
  const { locale } = useLocale();

  useEffect(() => {
    document.documentElement.lang = locale;
  }, [locale]);

  // A deployment may override product texts through CustomerI18nResources. The static catalog can't hold
  // those (they live in the backend's runtime resourceDir), so they are fetched and overlaid with highest
  // priority. Until they land, the static catalog shows — the product text, never a raw key — so this
  // needs no loading state; a failure (older backend without the endpoint) simply leaves the catalog as is.
  const { data: overrides } = useQuery({
    queryKey: ["i18n-customer-overrides", locale],
    queryFn: ({ signal }) => fetchCustomerI18nOverrides(locale, signal),
    staleTime: Infinity,
    gcTime: Infinity,
    retry: false,
  });

  const messages = useMemo(
    () => applyCustomerOverrides(MESSAGES[locale], overrides),
    [locale, overrides]
  );

  return (
    <NextIntlClientProvider
      locale={locale}
      messages={messages}
      // Explicit because there is no server config to supply it; without it
      // next-intl cannot resolve a time zone while prerendering the export.
      timeZone={DEFAULT_TIME_ZONE}
    >
      {children}
    </NextIntlClientProvider>
  );
}
