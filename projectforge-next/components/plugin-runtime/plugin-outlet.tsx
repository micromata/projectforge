"use client";

import { useEffect, useState, type ComponentType } from "react";
import { Spinner } from "@/components/shared/spinner";
import { installPluginRuntime } from "@/lib/plugin-runtime/bridge";

interface PluginDefinition {
  routes: { path: string; component: ComponentType }[];
}

/** Entry bundles are only loaded from here, whatever a registry would answer. */
const ASSET_PREFIX = "/next/plugins-assets/";

/** PoC: loads one plugin bundle at runtime and renders the component of its first route. */
export function PluginOutlet({ entry }: { entry: string }) {
  const [Page, setPage] = useState<ComponentType | null>(null);
  const [error, setError] = useState<string | null>(null);

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
      const definition: PluginDefinition = await module.activate(host);
      if (!cancelled) setPage(() => definition.routes[0].component);
    }
    load().catch((e: unknown) => {
      if (!cancelled) setError(e instanceof Error ? e.message : String(e));
    });
    return () => {
      cancelled = true;
    };
  }, [entry]);

  if (error) return <p role="alert" className="p-6 text-destructive">{error}</p>;
  if (!Page) return <Spinner />;
  return <Page />;
}
