"use client";

import { createContext, useContext, useMemo, type ReactNode } from "react";
import { NextIntlClientProvider, useLocale, useTimeZone } from "next-intl";
import { useQueries, useQuery } from "@tanstack/react-query";
import {
  applyCustomerOverrides,
  MESSAGES,
  normalizeLocale,
} from "@/i18n/config";
import {
  manifestProblem,
  usePluginManifests,
} from "@/lib/plugin-runtime/manifest";
import { fetchCustomerI18nOverrides } from "@/lib/rs/i18n";
import { fetchPluginMessages } from "@/lib/rs/plugin-frontend";

/**
 * The plugins whose texts are part of the texts handed down (or that have none, or failed): decided in the same render
 * that hands them down, so a page that waits for its id never renders before its keys exist.
 */
const TextsDoneContext = createContext<string[]>([]);

export function usePluginTextsDone(): string[] {
  return useContext(TextsDoneContext);
}

export const pluginMessagesQueryKey = (id: string, locale: string) =>
  ["plugin-messages", id, locale] as const;

/**
 * The texts of the plugins between the texts of the app and the customer's own texts (which keep the highest
 * priority, also for the keys of a plugin): every activated plugin with a frontend delivers its own, so a new text
 * needs no build of the app. A failure leaves the plugin without its texts rather than the app without a page.
 */
export function PluginMessagesProvider({ children }: { children: ReactNode }) {
  const locale = normalizeLocale(useLocale()) ?? "en";
  const timeZone = useTimeZone();
  const { data: manifests } = usePluginManifests();
  const valid = (manifests ?? []).filter((m) => manifestProblem(m) === null);
  // The same query as LocaleProvider's: one request, shared.
  const { data: customer } = useQuery({
    queryKey: ["i18n-customer-overrides", locale],
    queryFn: ({ signal }) => fetchCustomerI18nOverrides(locale, signal),
    staleTime: Infinity,
    gcTime: Infinity,
    retry: false,
  });
  // One flat map of all plugins and the ids that are done (structurally shared, so it only changes with the data).
  const { merged, done } = useQueries({
    queries: valid.map((manifest) => {
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
    combine: (results) => ({
      merged: Object.assign({}, ...results.map((r) => r.data ?? {})) as Record<
        string,
        string
      >,
      done: valid
        .filter((_, i) => results[i].isSuccess || results[i].isError)
        .map((m) => m.id),
    }),
  });
  const messages = useMemo(
    () =>
      applyCustomerOverrides(
        applyCustomerOverrides(MESSAGES[locale], merged),
        customer
      ),
    [locale, merged, customer]
  );
  return (
    <TextsDoneContext.Provider value={done}>
      <NextIntlClientProvider
        locale={locale}
        messages={messages}
        timeZone={timeZone}
      >
        {children}
      </NextIntlClientProvider>
    </TextsDoneContext.Provider>
  );
}
