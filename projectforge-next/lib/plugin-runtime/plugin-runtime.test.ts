import { describe, expect, it } from "vitest";
import {
  isPluginAssetUrl,
  manifestError,
  PLUGIN_SDK_VERSION,
} from "./manifest";
import { matchRoute } from "./route-context";
import type { PluginManifest } from "@/lib/rs/plugin-frontend";

const manifest = (over: Partial<PluginManifest> = {}): PluginManifest => ({
  id: "example",
  name: "Example",
  sdkVersion: PLUGIN_SDK_VERSION,
  entry: "/next/plugins-assets/example/1/plugin.js",
  css: ["/next/plugins-assets/example/1/plugin.css"],
  messages: { de: "/next/plugins-assets/example/1/de.json" },
  route: "/plugin/example",
  ...over,
});

describe("matchRoute", () => {
  it("matches static and param segments, ignoring slashes", () => {
    expect(matchRoute("/", "/")).toEqual({});
    expect(matchRoute("/items/:id", "/items/5/")).toEqual({ id: "5" });
    expect(matchRoute("/items/:id", "/items/a%20b")).toEqual({ id: "a b" });
    expect(matchRoute("/items/:id/edit", "/items/5/edit")).toEqual({ id: "5" });
  });

  it("does not match other shapes", () => {
    expect(matchRoute("/items/:id", "/items")).toBeNull();
    expect(matchRoute("/items/:id", "/items/5/edit")).toBeNull();
    expect(matchRoute("/items", "/other")).toBeNull();
  });
});

describe("manifestError", () => {
  it("accepts a manifest of the known sdk version below the assets", () => {
    expect(manifestError(manifest())).toBeNull();
  });

  it("refuses an unknown sdk version", () => {
    expect(manifestError(manifest({ sdkVersion: "0" }))).toContain("sdk");
  });

  it("refuses urls outside the assets or leaving them", () => {
    expect(
      manifestError(manifest({ entry: "https://evil.example/plugin.js" }))
    ).not.toBeNull();
    expect(
      manifestError(manifest({ entry: "/next/plugins-assets/../_next/x.js" }))
    ).not.toBeNull();
    expect(
      manifestError(manifest({ css: ["/other/plugin.css"] }))
    ).not.toBeNull();
    expect(
      manifestError(manifest({ messages: { de: "//evil/x.json" } }))
    ).not.toBeNull();
    expect(isPluginAssetUrl("/next/plugins-assets/example/plugin.js")).toBe(
      true
    );
  });
});
