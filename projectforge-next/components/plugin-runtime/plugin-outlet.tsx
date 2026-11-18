"use client";

import { useEffect, useState } from "react";
import { notFound, usePathname, useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { Spinner } from "@/components/shared/spinner";
import { usePluginTextsDone } from "@/components/plugin-runtime/plugin-messages-provider";
import {
  manifestProblem,
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
  const [, id = "", ...rest] = pathname.replace(/\/+$/, "").split("/").slice(1);
  // One instance per plugin: what was loaded for another one must not be shown (or fail) under this one's url.
  return <PluginPages key={id} id={id} subPath={`/${rest.join("/")}`} />;
}

function PluginPages({ id, subPath }: { id: string; subPath: string }) {
  const t = useTranslations("plugin.frontend");
  const router = useRouter();
  const { data: manifests, isError, isPending } = usePluginManifests();
  const manifest = manifests?.find((m) => m.id === id);
  const problem = manifest ? manifestProblem(manifest) : null;
  const [definition, setDefinition] = useState<PluginDefinition | null>(null);
  const [loadFailed, setLoadFailed] = useState(false);

  // The texts of the plugin are in the texts handed down before its first page renders (else a key would show).
  const textsDone = usePluginTextsDone().includes(id);

  useEffect(() => {
    if (!manifest || problem) return;
    let cancelled = false;
    loadPlugin(manifest)
      .then((loaded) => !cancelled && setDefinition(loaded))
      .catch((e: unknown) => {
        console.error(`Plugin ${manifest.id} failed to load`, e);
        if (!cancelled) setLoadFailed(true);
      });
    return () => {
      cancelled = true;
    };
  }, [manifest, problem]);

  const route = definition?.routes
    .map((r) => ({ Page: r.component, params: matchRoute(r.path, subPath) }))
    .find((r) => r.params);
  const match = route ? { Page: route.Page, params: route.params! } : null;
  const value = {
    params: match?.params ?? {},
    navigate: (path: string) => router.push(`${BASE}/${id}${path}`),
  };

  if (isError) return <Problem>{t("error.manifests")}</Problem>;
  if (isPending) return <Spinner />;
  if (!manifest) notFound();
  if (problem) {
    console.error(`Plugin ${id} is refused: ${problem}`);
    return <Problem>{t(`error.${problem}`, { name: manifest.name })}</Problem>;
  }
  if (loadFailed)
    return <Problem>{t("error.load", { name: manifest.name })}</Problem>;
  if (!definition || !textsDone) return <Spinner />;
  if (!match) notFound();
  return (
    <PluginRouteContext.Provider value={value}>
      <match.Page />
    </PluginRouteContext.Provider>
  );
}
