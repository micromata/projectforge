import { installPluginRuntime } from "./bridge";
import type { PluginManifest } from "@/lib/rs/plugin-frontend";

export interface PluginDefinition {
  routes: { path: string; component: React.ComponentType }[];
}

const loaded = new Map<string, Promise<PluginDefinition>>();

function addStyleSheet(url: string) {
  if (document.querySelector(`link[data-plugin-css="${url}"]`)) return;
  const link = document.createElement("link");
  link.rel = "stylesheet";
  link.href = url;
  link.dataset.pluginCss = url;
  document.head.appendChild(link);
}

/** Loads the bundle of a plugin once (styles first), and activates it with the bridge. */
export function loadPlugin(
  manifest: PluginManifest
): Promise<PluginDefinition> {
  let promise = loaded.get(manifest.id);
  if (!promise) {
    promise = (async () => {
      manifest.css.forEach(addStyleSheet);
      const host = installPluginRuntime();
      // A native browser import, not a module of this build: the bundler must leave it alone.
      const bundle = await import(
        /* webpackIgnore: true */ /* turbopackIgnore: true */ manifest.entry
      );
      return (await bundle.activate(host)) as PluginDefinition;
    })();
    // A failed load is tried again on the next visit.
    promise.catch(() => loaded.delete(manifest.id));
    loaded.set(manifest.id, promise);
  }
  return promise;
}
