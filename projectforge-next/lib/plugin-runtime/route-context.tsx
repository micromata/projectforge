"use client";

import { createContext, useContext } from "react";

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

/** `/items/:id` against `/items/5` -> `{ id: "5" }`, or null. A trailing slash does not matter. */
export function matchRoute(
  pattern: string,
  path: string
): Record<string, string> | null {
  const split = (p: string) => p.split("/").filter(Boolean);
  const want = split(pattern);
  const have = split(path);
  if (want.length !== have.length) return null;
  const params: Record<string, string> = {};
  for (let i = 0; i < want.length; i++) {
    if (want[i].startsWith(":"))
      params[want[i].slice(1)] = decodeURIComponent(have[i]);
    else if (want[i] !== have[i]) return null;
  }
  return params;
}
