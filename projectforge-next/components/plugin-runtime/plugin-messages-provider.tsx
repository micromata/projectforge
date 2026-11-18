"use client";

import { useMemo, type ReactNode } from "react";
import {
  NextIntlClientProvider,
  useLocale,
  useMessages,
  useTimeZone,
} from "next-intl";
import { useQueries } from "@tanstack/react-query";
import { applyCustomerOverrides } from "@/i18n/config";
import { usePluginManifests } from "@/lib/plugin-runtime/manifest";
import { fetchPluginMessages } from "@/lib/rs/plugin-frontend";

export const pluginMessagesQueryKey = (id: string, locale: string) =>
  ["plugin-messages", id, locale] as const;

/**
 * The texts of the plugins on top of the texts of the app (same overlay as the customer's own texts): every
 * activated plugin with a frontend delivers its own, so a new text needs no build of the app. A failure leaves
 * the plugin without its texts rather than the app without a page.
 */
export function PluginMessagesProvider({ children }: { children: ReactNode }) {
  const locale = useLocale();
  const base = useMessages() as Record<string, unknown>;
  const timeZone = useTimeZone();
  const { data: manifests } = usePluginManifests();
  // One flat map of all plugins (structurally shared, so it only changes with the data).
  const merged = useQueries({
    queries: (manifests ?? []).map((manifest) => {
      const url = manifest.messages[locale] ?? manifest.messages.en;
      return {
        queryKey: pluginMessagesQueryKey(manifest.id, locale),
        queryFn: ({ signal }: { signal: AbortSignal }) =>
          url ? fetchPluginMessages(url, signal) : Promise.resolve({}),
        staleTime: Infinity,
        gcTime: Infinity,
        retry: false,
      };
    }),
    combine: (results) =>
      Object.assign({}, ...results.map((r) => r.data ?? {})) as Record<
        string,
        string
      >,
  });
  const messages = useMemo(
    () => applyCustomerOverrides(base, merged),
    [base, merged]
  );
  return (
    <NextIntlClientProvider
      locale={locale}
      messages={messages}
      timeZone={timeZone}
    >
      {children}
    </NextIntlClientProvider>
  );
}
