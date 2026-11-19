import { useQuery } from "@tanstack/react-query";
import {
  fetchPluginManifests,
  PLUGIN_MANIFESTS_QUERY_KEY,
  type PluginManifest,
} from "@/lib/rs/plugin-frontend";

/** The version of the plugin sdk (the bridge) this app speaks; a bundle built against another one is refused. */
export const PLUGIN_SDK_VERSION = "1";

/** Entry bundles, styles and texts are only loaded from here, whatever a manifest says. */
export const PLUGIN_ASSET_PREFIX = "/next/plugins-assets/";

export function isPluginAssetUrl(url: string): boolean {
  return url.startsWith(PLUGIN_ASSET_PREFIX) && !url.split("/").includes("..");
}

/** The frontends of the activated plugins; the answer only changes with a restart. */
export function usePluginManifests() {
  return useQuery({
    queryKey: PLUGIN_MANIFESTS_QUERY_KEY,
    queryFn: ({ signal }) => fetchPluginManifests(signal),
    staleTime: Infinity,
    gcTime: Infinity,
  });
}

/** Why the app refuses to load a plugin: it was built for another sdk, or its urls leave the assets. */
export type ManifestProblem = "incompatible" | "assets";

export function manifestProblem(
  manifest: PluginManifest
): ManifestProblem | null {
  if (manifest.sdkVersion !== PLUGIN_SDK_VERSION) return "incompatible";
  const urls = [
    manifest.entry,
    ...manifest.css,
    ...Object.values(manifest.messages),
  ];
  return urls.every(isPluginAssetUrl) ? null : "assets";
}
