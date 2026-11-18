// Build of a frontend plugin against the bridge of projectforge-next (lib/plugin-runtime/bridge.ts).
// Not an npm package of its own (yet): a plugin repository loads it from its ProjectForge checkout, see README.md.
import { createRequire } from "node:module";
import { readFileSync } from "node:fs";
import { join } from "node:path";

const BRIDGE = "globalThis.__HOST_PLUGIN_RUNTIME__";

// Resolved from the plugin that is built (the key list of a package), not from this directory.
const requireFromPlugin = createRequire(join(process.cwd(), "package.json"));

/** Named exports of a package, to be re-exported from the instance of the app. */
function names(pkg) {
  return Object.keys(requireFromPlugin(pkg)).filter(
    (k) => k !== "default" && k !== "__esModule" && /^[A-Za-z_$][\w$]*$/.test(k)
  );
}

function adapter(member, pkg, withDefault) {
  const lines = [`const m = ${BRIDGE}.${member};`];
  if (withDefault) lines.push("export default m;");
  for (const n of names(pkg)) lines.push(`export const ${n} = m.${n};`);
  return lines.join("\n");
}

/** The shared dependencies: module specifier -> member of the bridge, package whose export names it has. */
const SHARED = {
  react: ["react", "react", true],
  "react/jsx-runtime": ["jsxRuntime", "react/jsx-runtime", false],
  "@tanstack/react-query": ["reactQuery", "@tanstack/react-query", false],
  "next-intl": ["nextIntl", "next-intl", false],
};

/**
 * Maps the shared dependencies to the instances of the app: the plugin imports `react` as usual but never bundles it.
 * `@host/plugin-runtime` is the bridge itself (`ui`, `api`, `hooks`, ...).
 */
export function hostBridge() {
  const modules = Object.fromEntries(
    Object.entries(SHARED).map(([id, [member, pkg, withDefault]]) => [
      id,
      () => adapter(member, pkg, withDefault),
    ])
  );
  modules["@host/plugin-runtime"] = () =>
    [
      `export default ${BRIDGE};`,
      `export const ui = ${BRIDGE}.ui;`,
      `export const api = ${BRIDGE}.api;`,
      `export const hooks = ${BRIDGE}.hooks;`,
    ].join("\n");
  return {
    name: "host-bridge",
    enforce: "pre",
    resolveId: (id) => (id in modules ? `\0host:${id}` : null),
    load: (id) => (id.startsWith("\0host:") ? modules[id.slice(6)]() : null),
  };
}

/**
 * The vite config of a plugin: one ES module `plugin.js` in `outDir`, no React in it.
 * @param {{ entry?: string, outDir: string }} options
 */
export function pluginViteConfig({ entry = "src/index.tsx", outDir }) {
  return {
    plugins: [hostBridge()],
    esbuild: { jsx: "automatic" },
    define: { "process.env.NODE_ENV": JSON.stringify("production") },
    build: {
      target: "es2022",
      outDir,
      emptyOutDir: true,
      lib: { entry, formats: ["es"], fileName: () => "plugin.js" },
    },
  };
}

/** Fails the build if the bundle carries an implementation of React of its own (it must use the app's). */
export function checkBundle(file) {
  const code = readFileSync(file, "utf8");
  const markers = [
    "react.element",
    "react.transitional.element",
    "__SECRET_INTERNALS_DO_NOT_USE",
    "ReactCurrentDispatcher",
  ];
  const found = markers.filter((m) => code.includes(m));
  if (found.length > 0) {
    throw new Error(`${file} bundles React (found ${found.join(", ")}).`);
  }
  console.log(`${file}: ${code.length} bytes, no bundled React`);
}
