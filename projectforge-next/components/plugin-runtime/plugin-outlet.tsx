"use client";

import { useEffect, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import { useLocale } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { Spinner } from "@/components/shared/spinner";
import { pluginMessagesQueryKey } from "@/components/plugin-runtime/plugin-messages-provider";
import {
  manifestError,
  usePluginManifests,
} from "@/lib/plugin-runtime/manifest";
import {
  loadPlugin,
  type PluginDefinition,
} from "@/lib/plugin-runtime/load-plugin";
import {
  matchRoute,
  PluginRouteContext,
} from "@/lib/plugin-runtime/route-context";
import { fetchPluginMessages } from "@/lib/rs/plugin-frontend";

const BASE = "/plugin";

function Problem({ children }: { children: React.ReactNode }) {
  return (
    <p role="alert" className="p-6 text-destructive">
      {children}
    </p>
  );
}

/**
 * The pages of a plugin with a frontend of its own, `/plugin/<id>/<route of the plugin>`. The app knows nothing of the
 * plugin but its manifest (PluginFrontendRest): it loads the bundle, and matches the url against the routes the bundle
 * names. The plugin never sees the router of the app, it gets the params of its route and `navigate`.
 */
export function PluginOutlet() {
  const pathname = usePathname();
  const router = useRouter();
  const locale = useLocale();
  const [, id = "", ...rest] = pathname.replace(/\/+$/, "").split("/").slice(1);
  const subPath = `/${rest.join("/")}`;
  const { data: manifests, isError, isPending } = usePluginManifests();
  const manifest = manifests?.find((m) => m.id === id);
  const error = manifest ? manifestError(manifest) : null;
  const [definition, setDefinition] = useState<PluginDefinition | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);

  // The texts of the plugin are there before its first page renders (a missing text would show as its key).
  const messagesUrl = manifest?.messages[locale] ?? manifest?.messages.en;
  const { isPending: messagesPending } = useQuery({
    queryKey: pluginMessagesQueryKey(id, locale),
    queryFn: ({ signal }) => fetchPluginMessages(messagesUrl!, signal),
    enabled: !!messagesUrl && !error,
    staleTime: Infinity,
    retry: false,
  });

  useEffect(() => {
    if (!manifest || error) return;
    let cancelled = false;
    loadPlugin(manifest)
      .then((loaded) => !cancelled && setDefinition(loaded))
      .catch(
        (e: unknown) =>
          !cancelled && setLoadError(e instanceof Error ? e.message : String(e))
      );
    return () => {
      cancelled = true;
    };
  }, [manifest, error]);

  const route = definition?.routes
    .map((r) => ({ Page: r.component, params: matchRoute(r.path, subPath) }))
    .find((r) => r.params);
  const match = route ? { Page: route.Page, params: route.params! } : null;

  const value = {
    params: match?.params ?? {},
    navigate: (path: string) => router.push(`${BASE}/${id}${path}`),
  };

  if (isError) return <Problem>The plugins could not be loaded.</Problem>;
  if (isPending) return <Spinner />;
  if (!manifest) return <Problem>Not found</Problem>;
  if (error ?? loadError) return <Problem>{error ?? loadError}</Problem>;
  if (!definition || (!!messagesUrl && messagesPending)) return <Spinner />;
  if (!match) return <Problem>Not found</Problem>;
  return (
    <PluginRouteContext.Provider value={value}>
      <match.Page />
    </PluginRouteContext.Provider>
  );
}
