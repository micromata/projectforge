"use client";

import { useCallback, useEffect, useSyncExternalStore } from "react";
import { NextIntlClientProvider } from "next-intl";
import {
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
      readCookieLocale() ?? normalizeLocale(navigator.language) ?? DEFAULT_LOCALE;
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
  const locale = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);

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

  return (
    <NextIntlClientProvider
      locale={locale}
      messages={MESSAGES[locale]}
      // Explicit because there is no server config to supply it; without it
      // next-intl cannot resolve a time zone while prerendering the export.
      timeZone={DEFAULT_TIME_ZONE}
    >
      {children}
    </NextIntlClientProvider>
  );
}
