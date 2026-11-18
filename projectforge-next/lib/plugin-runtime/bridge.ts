import * as React from "react";
import * as jsxRuntime from "react/jsx-runtime";
import * as reactQuery from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { SectionCard } from "@/components/shared/section-card";
import { getQueryClient } from "@/lib/query-client";
import { request } from "@/lib/rs/client";
import { PLUGIN_SDK_VERSION } from "@/lib/plugin-runtime/manifest";
import { usePluginRoute } from "@/lib/plugin-runtime/route-context";

/**
 * The host side of the runtime frontend plugin PoC: what an independently built plugin bundle gets of
 * this app. A plugin build maps its imports of `react`, `react/jsx-runtime`, `@tanstack/react-query` and
 * `@host/plugin-runtime` to small adapters reading `globalThis.__HOST_PLUGIN_RUNTIME__`, so a plugin
 * renders with the very React and QueryClient instance of this app.
 */
export interface HostPluginRuntime {
  sdkVersion: string;
  react: typeof React;
  jsxRuntime: typeof jsxRuntime;
  reactQuery: typeof reactQuery;
  ui: { Button: typeof Button; SectionCard: typeof SectionCard };
  api: { request: typeof request };
  hooks: { usePluginRoute: typeof usePluginRoute };
  /** Identity checks of the PoC (see the plugin's page), not part of a later SDK. */
  debug: { react: typeof React; queryClient: reactQuery.QueryClient };
}

declare global {
  var __HOST_PLUGIN_RUNTIME__: HostPluginRuntime | undefined;
}

export function installPluginRuntime(): HostPluginRuntime {
  const existing = globalThis.__HOST_PLUGIN_RUNTIME__;
  if (existing) return existing;
  const runtime: HostPluginRuntime = {
    sdkVersion: PLUGIN_SDK_VERSION,
    react: React,
    jsxRuntime,
    reactQuery,
    ui: { Button, SectionCard },
    api: { request },
    hooks: { usePluginRoute },
    debug: { react: React, queryClient: getQueryClient() },
  };
  globalThis.__HOST_PLUGIN_RUNTIME__ = runtime;
  return runtime;
}
