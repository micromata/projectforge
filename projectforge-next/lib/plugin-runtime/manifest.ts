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

export function manifestError(manifest: PluginManifest): string | null {
  if (manifest.sdkVersion !== PLUGIN_SDK_VERSION) {
    return `Plugin ${manifest.id} was built for sdk ${manifest.sdkVersion}, this app speaks ${PLUGIN_SDK_VERSION}.`;
  }
  const urls = [
    manifest.entry,
    ...manifest.css,
    ...Object.values(manifest.messages),
  ];
  const bad = urls.find((url) => !isPluginAssetUrl(url));
  return bad
    ? `Plugin ${manifest.id} points outside of its assets: ${bad}`
    : null;
}
