/**
 * The Plugins (administration) page (`org.projectforge.rest.PluginAdminRest`), successor of Wicket's
 * `PluginListPage`. A non-entity, standalone action page, so it has its own small client here rather
 * than going through `fetchList` / the entity plumbing (as `./system.ts` does).
 *
 * Every endpoint is admin-only and self-checks on the backend (there is no DAO access backstop). The
 * toggle answers with a translated `{ message }` (the "takes effect after restart" note).
 */

import { request } from "./client";
import type { PluginListData } from "@/components/features/plugins/types";

/** The result of the toggle action: a text already translated by the backend. */
export interface PluginMessageResponse {
  message: string;
}

export function fetchPluginList(signal?: AbortSignal): Promise<PluginListData> {
  return request<PluginListData>("/rs/pluginList", { method: "GET" }, signal);
}

/** Activates or deactivates a plugin in the configuration; the change takes effect after a restart. */
export function setPluginActivated(
  id: string,
  activate: boolean
): Promise<PluginMessageResponse> {
  return request<PluginMessageResponse>("/rs/pluginList/setActivated", {
    method: "POST",
    body: JSON.stringify({ id, activate }),
  });
}
