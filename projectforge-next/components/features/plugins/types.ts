/** Types of the Plugins (administration) page (`/next/plugins`), successor of Wicket's `PluginListPage`. */

/** One available plugin, as `PluginAdminRest.PluginItem` serializes it. */
export interface PluginItem {
  /** The plugin id, e.g. `merlin`. */
  id: string;
  /** The plugin's display name. */
  name: string;
  /** The plugin's description. */
  description: string;
  /** Whether the plugin is activated in the database configuration (the toggle's state). */
  active: boolean;
  /**
   * Whether the plugin is forced active via `projectforge.plugins.ensure-active` and therefore cannot be
   * deactivated here.
   */
  ensureActive: boolean;
}

/** Initial state of the page, as `PluginAdminRest.PluginListData` serializes it. */
export interface PluginListData {
  /** The available plugins (activated and not). */
  plugins: PluginItem[];
  /** The ids forced active via `projectforge.plugins.ensure-active`, for the info banner. */
  ensureActivePluginIds: string[];
}
