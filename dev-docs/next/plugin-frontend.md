# Frontend plugins loaded at runtime

A plugin can bring its own pages without a rebuild of projectforge-next: a bundle of native ES modules, built independently
(Vite), shipped in the plugin jar and loaded by the app in the browser. The app knows the plugin only by its manifest.
Status: the seam is in place (manifest, outlet, texts, bridge, build package); the first real plugin to use it is the
CVE-Pilot, its pages still live in this package until they have moved (see the plan in the plugin's repository).

## How it works

1. **Declare.** The backend plugin overrides `AbstractPlugin.frontend` with a `PluginFrontend`: `sdkVersion`, `assetDir`
   (below `static/next/plugins-assets/`, put the version in it), `entry` (default `plugin.js`), `css`, `messages` per locale.
   Spring serves the files from the plugin jar (`classpath:/static/next/`), same origin, so the content security policy needs
   no change. A missing file is a real 404.
2. **Manifest.** `GET /rs/pluginFrontend` (`PluginFrontendRest`) lists the activated plugins with a frontend. The same for
   every user: what a user may do inside the pages is decided by the REST endpoints of the plugin, which check access themselves.
   `FeaturePathIndex` switches `/next/plugin/<id>/**` off with the plugin.
3. **Outlet.** One route `app/(authenticated)/plugin/[[...path]]` (a single prerendered placeholder; the spa-shell-map of Spring
   answers every deeper url with it). `PluginOutlet` reads plugin id and route from the pathname, looks the manifest up, refuses
   an unknown `sdkVersion` or urls outside `/next/plugins-assets/`, adds the styles, loads the bundle once with a native
   `import()` (`/* webpackIgnore */ /* turbopackIgnore */`: not a module of this build) and calls its `activate(host)`.
   The returned routes (`/items/:id`) are matched by the app; the page gets its params and `navigate` through `usePluginRoute`,
   never the Next router.
4. **Texts.** `PluginMessagesProvider` (authenticated layout) fetches the json of each plugin for the locale (flat dotted keys)
   and overlays it on the app's texts like the customer texts; the outlet waits for the texts of its plugin before it renders.
   A new text needs no build of the app.
5. **Bridge.** `lib/plugin-runtime/bridge.ts` puts `react`, `react/jsx-runtime`, `@tanstack/react-query`, `next-intl`, selected
   components (`ui`), `api.request` and `hooks.usePluginRoute` on `globalThis.__HOST_PLUGIN_RUNTIME__`. The plugin imports the
   packages as usual; its build maps them to the bridge (`packages/plugin-vite`), so it renders with the **same** React and the
   same providers, QueryClient and locale as the app. What is on the bridge is the contract: change it like a public api and
   raise `PLUGIN_SDK_VERSION` (`lib/plugin-runtime/manifest.ts`, `packages/plugin-sdk`) for a breaking change.

## Pages and old links of a plugin

A plugin that migrates REST categories to next pages declares them itself, not the core: `AbstractPlugin.nextPages` (category ->
`PageRoutes.Page`, the routes of its list/edit/new pages, e.g. below `plugin/<id>`) and `renamedNextSections` (former route -> current
route, whose sub pages follow). They join `PageRoutes` when the plugin is initialized, i.e. only if it is activated; menu entries,
links of notifications and the redirects of `OrphanedLinkFilter` then work as for a page of the core.

## Building a plugin

See `packages/plugin-vite/README.md` (build, `checkBundle`) and `packages/plugin-sdk` (types). The plugin repository takes both
from its ProjectForge checkout (git submodule, or `PF_CORE_DIR` for development); there is no npm registry. Plugin css must be
its own (Tailwind does not see plugin sources, use the components of the app where you can).

## Rules

- A plugin is trusted code of the same origin: it runs with the privileges of the page. Only first-party plugins for now.
- No `unsafe-eval`, no external hosts: the bundle comes from `/next/plugins-assets/`, nothing else is loaded.
- Plugin REST endpoints check access themselves (`@AccessChecked`); hiding a page protects nothing.
- Tests: `lib/plugin-runtime/plugin-runtime.test.ts` (route matching, manifest checks), `e2e/plugin-poc.spec.ts` (needs a plugin
  jar with a bundle; the CVE-Pilot repository's PoC bundle serves it), `FeaturePathIndexTest`, `PluginFrontendRestTest`.

## Open points

- The names a plugin build re-exports from `react`, `@tanstack/react-query` and `next-intl` come from the plugin's own `node_modules`
  (`packages/plugin-vite`), not from the bridge: another version there can export a name the app lacks (`undefined` at runtime, no
  build error). To do: check the names against `globalThis.__HOST_PLUGIN_RUNTIME__` when the plugin is activated, and pin the list of
  bridge exports in a test.
- The core still knows the CVE-Pilot in `ExternalPlugins` (routes blocked while the plugin is missing, the prefix of its field metadata, `PluginAdminService.PLUGIN_CVE_RADAR_ID`),
  in the texts module `plugins/org.projectforge.plugins.cveradar` (and `gen`, which exports them to the catalogs of the app) and in the pages `app/(authenticated)/{cvepilot,cvrcase}`;
  all of it goes when the pages have moved into the plugin.
- `HostPluginRuntime` is not yet typed for plugins (`packages/plugin-sdk` has the page contract only); the typed pieces come with the
  first pages that need them.
- Plugin css is untested; Tailwind does not see plugin sources.
