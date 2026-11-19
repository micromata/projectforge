# @projectforge/plugin-vite

Build of a frontend plugin (see `PluginFrontend` in the backend, `lib/plugin-runtime` here). The plugin imports `react`,
`react/jsx-runtime`, `@tanstack/react-query`, `next-intl` and `@host/plugin-runtime` as usual; the build maps them to the
instances of the app (the bridge, `lib/plugin-runtime/bridge.ts`), so nothing of them is bundled and the plugin renders in
the React tree of the app, with its providers.

```ts
// vite.config.ts of the plugin; PF_CORE_DIR is the ProjectForge checkout (the submodule or -PpfCoreDir)
const core =
  process.env.PF_CORE_DIR ?? resolve(__dirname, "../../projectforge");
const { pluginViteConfig } = await import(
  `${core}/projectforge-next/packages/plugin-vite/index.mjs`
);
export default defineConfig(
  pluginViteConfig({
    outDir: "../src/main/resources/static/next/plugins-assets/<id>",
  })
);
```

After the build, `checkBundle(<outDir>/plugin.js)` fails if React ended up in the bundle. The plugin needs `react`,
`@tanstack/react-query` and `next-intl` as dev dependencies, only to know their export names.
