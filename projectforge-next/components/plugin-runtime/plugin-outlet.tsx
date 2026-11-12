"use client";

import { useEffect, useMemo, useState, type ComponentType } from "react";
import { usePathname, useRouter } from "next/navigation";
import { Spinner } from "@/components/shared/spinner";
import { installPluginRuntime } from "@/lib/plugin-runtime/bridge";
import {
  matchRoute,
  PluginRouteContext,
} from "@/lib/plugin-runtime/route-context";

interface PluginDefinition {
  routes: { path: string; component: ComponentType }[];
}

/** Entry bundles are only loaded from here, whatever a registry would answer. */
const ASSET_PREFIX = "/next/plugins-assets/";

/**
 * PoC: loads one plugin bundle at runtime and renders the page of its route matching the url below `basePath`.
 * The host owns the url (the plugin never sees the router): it matches, and hands params and `navigate` down.
 */
export function PluginOutlet({
  entry,
  basePath,
}: {
  entry: string;
  basePath: string;
}) {
  const [definition, setDefinition] = useState<PluginDefinition | null>(null);
  const [error, setError] = useState<string | null>(null);
  const pathname = usePathname();
  const router = useRouter();

  useEffect(() => {
    let cancelled = false;
    async function load() {
      if (!entry.startsWith(ASSET_PREFIX) || entry.includes("..")) {
        throw new Error(`Plugin entry outside of ${ASSET_PREFIX}: ${entry}`);
      }
      const host = installPluginRuntime();
      // A native browser import, not a module of this build: the bundler must leave it alone.
      const module = await import(
        /* webpackIgnore: true */ /* turbopackIgnore: true */ entry
      );
      const loaded: PluginDefinition = await module.activate(host);
      if (!cancelled) setDefinition(loaded);
    }
    load().catch((e: unknown) => {
      if (!cancelled) setError(e instanceof Error ? e.message : String(e));
    });
    return () => {
      cancelled = true;
    };
  }, [entry]);

  const match = useMemo(() => {
    if (!definition) return null;
    const path = pathname.startsWith(basePath) ? pathname.slice(basePath.length) : "/";
    for (const route of definition.routes) {
      const params = matchRoute(route.path, path);
      if (params) return { Page: route.component, params };
    }
    return null;
  }, [definition, pathname, basePath]);

  const value = useMemo(
    () => ({
      params: match?.params ?? {},
      navigate: (path: string) => router.push(`${basePath}${path}`),
    }),
    [match, router, basePath]
  );

  if (error) return <p role="alert" className="p-6 text-destructive">{error}</p>;
  if (!definition) return <Spinner />;
  if (!match) return <p role="alert" className="p-6">Not found</p>;
  return (
    <PluginRouteContext.Provider value={value}>
      <match.Page />
    </PluginRouteContext.Provider>
  );
}
