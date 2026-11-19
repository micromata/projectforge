// The contract between the app and a frontend plugin (sdk version 1). Types only: the plugin gets the running
// implementation from the app (lib/plugin-runtime/bridge.ts) when `activate` is called.
import type { ComponentType } from "react";

/** The version of this contract; the manifest of the plugin names the one it was built for. */
export declare const PLUGIN_SDK_VERSION: "1";

/** A page of a plugin: `path` is relative to the url of the plugin, `:name` segments are params. */
export interface PluginPage {
  path: string;
  component: ComponentType;
}

/** What `activate` of the entry module returns. */
export interface PluginDefinition {
  routes: PluginPage[];
}

/** The params of the matched route and the way to another page of the plugin (`/items/5`). */
export interface PluginRoute {
  params: Record<string, string>;
  navigate: (path: string) => void;
}

/** The entry module of a plugin: `export function activate(host) { return { routes: [...] } }`. */
export type PluginActivate = (
  host: unknown
) => PluginDefinition | Promise<PluginDefinition>;
