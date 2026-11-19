import { request } from "./client";

/** The frontend of an activated plugin, see PluginFrontendRest. All urls are below `/next/plugins-assets/`. */
export interface PluginManifest {
  id: string;
  name: string;
  sdkVersion: string;
  entry: string;
  css: string[];
  /** Locale -> url of the json file with the texts (flat dotted keys). */
  messages: Record<string, string>;
  /** The url of the pages of the plugin below the app, e.g. `/plugin/example`. */
  route: string;
}

export const PLUGIN_MANIFESTS_QUERY_KEY = ["plugin-frontend"] as const;

export function fetchPluginManifests(
  signal?: AbortSignal
): Promise<PluginManifest[]> {
  return request<PluginManifest[]>(
    "/rs/pluginFrontend",
    { method: "GET" },
    signal
  );
}

/** Texts of a plugin for a locale: a flat map of dotted keys to texts. */
export function fetchPluginMessages(
  url: string,
  signal?: AbortSignal
): Promise<Record<string, string>> {
  return request<Record<string, string>>(url, { method: "GET" }, signal);
}
