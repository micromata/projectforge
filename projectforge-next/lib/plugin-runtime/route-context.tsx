"use client";

import { createContext, useContext } from "react";
import { matchRoute as appMatchRoute } from "@/lib/route-params";

/** What a plugin page knows of its place: the params of the matched route and how to move on. */
export interface PluginRoute {
  params: Record<string, string>;
  /** A path inside the plugin (`/items/5`); the host adds its own prefix. */
  navigate: (path: string) => void;
}

export const PluginRouteContext = createContext<PluginRoute | null>(null);

export function usePluginRoute(): PluginRoute {
  const route = useContext(PluginRouteContext);
  if (!route) throw new Error("usePluginRoute outside of a plugin page");
  return route;
}

/**
 * `/items/:id` against `/items/5` -> `{ id: "5" }`, or null. A trailing slash does not matter. The matcher of the
 * app (lib/route-params), whose pattern writes a param as `[id]`; a plugin writes it as `:id`.
 */
export function matchRoute(
  pattern: string,
  path: string
): Record<string, string> | null {
  return appMatchRoute(pattern.replace(/:(\w+)/g, "[$1]"), path) as Record<
    string,
    string
  > | null;
}
