import type { APIRequestContext } from "@playwright/test";

/**
 * Whether the plugin `id` is activated on this instance (`GET /rs/pluginList`, PluginAdminRest).
 *
 * For the spec of a plugin's page, which skips on an instance that doesn't run the plugin — a freshly
 * seeded slot activates only some of them — rather than failing over a page the backend refuses. True
 * where the account may not read the list (admin group only): the spec then runs, and says so if it can't.
 */
export async function isPluginActive(
  request: APIRequestContext,
  id: string
): Promise<boolean> {
  const response = await request.get("/rs/pluginList", {
    headers: { "X-PF-Frontend": "next" },
  });
  if (!response.ok()) return true;
  const { plugins = [] } = (await response.json()) as {
    plugins?: { id: string; active: boolean; ensureActive: boolean }[];
  };
  return plugins.some(
    (plugin) => plugin.id === id && (plugin.active || plugin.ensureActive)
  );
}
