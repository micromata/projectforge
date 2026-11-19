# Frontend plugins loaded at runtime

A plugin can bring its own pages without a rebuild of projectforge-next: a bundle of native ES modules, built independently
(Vite), shipped in the plugin jar and loaded by the app in the browser. The app knows the plugin only by its manifest.
Status: in use: the pages of the CVE-Pilot are the frontend of its plugin (`/plugin/cveradar/**`). Plan and history in the
plugin's repository (`docs/runtime-frontend-plugin-plan.md`).

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

## What a plugin imports

`@/components/shared/page-shell`, `@/lib/format`, `next/navigation`, `zod`, ... in the source of a plugin are the modules of the app
listed in `lib/plugin-runtime/modules.ts`: the build maps them to the instances of the app (`modules` of the bridge), so the plugin
uses the app's own components, hooks and clients and their context. A file of the plugin at the same path wins (`@/lib/rs/...` of the
plugin itself); anything else is a build error ("neither a file of the plugin nor a module the app offers"). The list is the contract,
pinned by `modules.test.ts`.

## Open points

- The texts of the plugin are its own: the bundle is in the plugin jar, the build of its frontend generates one catalog per
  locale (all keys, flat) and names them in the manifest (`messages`); the app merges them while the plugin is active. No
  `gen` run here for a new text of the plugin.
- The e2e specs of the plugin (`e2e/cve-pilot-*.spec.ts`) are here, they need the infrastructure of this package.
- The plugin build needs `npm ci` of this package (the names a package exports are read from its `node_modules`).
- `HostPluginRuntime` is only typed for the bridge itself; the plugin source is type-checked against the app's files, not yet in its build.
- Plugin css is untested; Tailwind does not see plugin sources.
