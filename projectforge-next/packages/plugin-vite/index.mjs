// Build of a frontend plugin against the bridge of projectforge-next (lib/plugin-runtime/bridge.ts).
// Not an npm package of its own (yet): a plugin repository loads it from its ProjectForge checkout, see README.md.
import { createRequire } from "node:module";
import { existsSync, readFileSync, statSync } from "node:fs";
import { dirname, join, resolve } from "node:path";

const BRIDGE = "globalThis.__HOST_PLUGIN_RUNTIME__";

// The names a package exports are those of the copy the app was built with (its node_modules), not those of a copy the plugin
// might have: what the app hands out is what counts.
const requires = new Map();
function requireFromApp(coreDir) {
  if (!requires.has(coreDir)) {
    requires.set(
      coreDir,
      createRequire(join(coreDir, "projectforge-next", "package.json"))
    );
  }
  return requires.get(coreDir);
}

/** Named exports of a package, to be re-exported from the instance of the app. */
function names(pkg, coreDir) {
  return Object.keys(requireFromApp(coreDir)(pkg)).filter(
    (k) => k !== "default" && k !== "__esModule" && /^[A-Za-z_$][\w$]*$/.test(k)
  );
}

function adapter(member, pkg, withDefault, coreDir) {
  const lines = [`const m = ${BRIDGE}.${member};`];
  if (withDefault) lines.push("export default m;");
  for (const n of names(pkg, coreDir))
    lines.push(`export const ${n} = m.${n};`);
  return lines.join("\n");
}

/** The shared dependencies: module specifier -> member of the bridge, package whose export names it has. */
const SHARED = {
  react: ["react", "react", true],
  "react-dom": ["reactDom", "react-dom", true],
  "react/jsx-runtime": ["jsxRuntime", "react/jsx-runtime", false],
  "@tanstack/react-query": ["reactQuery", "@tanstack/react-query", false],
  "next-intl": ["nextIntl", "next-intl", false],
};

/**
 * Maps the shared dependencies to the instances of the app: the plugin imports `react` as usual but never bundles it.
 * `@host/plugin-runtime` is the bridge itself (`ui`, `api`, `hooks`, ...).
 */
export function hostBridge({ coreDir }) {
  const modules = Object.fromEntries(
    Object.entries(SHARED).map(([id, [member, pkg, withDefault]]) => [
      id,
      () => adapter(member, pkg, withDefault, coreDir),
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
    // The precise check: no module of react, react-dom or the scheduler ended up in the bundle (a library importing
    // `react/jsx-dev-runtime` or a file of react directly would bring a second React along).
    generateBundle(_options, bundle) {
      const bundled = Object.values(bundle)
        .flatMap((chunk) =>
          chunk.type === "chunk" ? Object.keys(chunk.modules) : []
        )
        .filter((id) =>
          /[\\/]node_modules[\\/](react|react-dom|scheduler)[\\/]/.test(id)
        );
      if (bundled.length > 0) {
        throw new Error(
          `The plugin bundles React (it must use the one of the app): ${bundled.slice(0, 3).join(", ")}`
        );
      }
    },
    load: (id) => (id.startsWith("\0host:") ? modules[id.slice(6)]() : null),
  };
}

const EXTENSIONS = ["", ".ts", ".tsx", "/index.ts", "/index.tsx"];

function firstFile(base) {
  return EXTENSIONS.map((ext) => base + ext).find(
    (f) => existsSync(f) && statSync(f).isFile()
  );
}

/** The value exports (no types) of a module of the app, following `export * from` and `export { x } from`. */
function exportsOf(file, coreDir, seen = new Set()) {
  if (seen.has(file)) return new Set();
  seen.add(file);
  const ts = requireFromApp(coreDir)("typescript");
  const source = ts.createSourceFile(
    file,
    readFileSync(file, "utf8"),
    ts.ScriptTarget.Latest,
    true
  );
  const names = new Set();
  const isExported = (node) =>
    node.modifiers?.some((m) => m.kind === ts.SyntaxKind.ExportKeyword) &&
    !node.modifiers.some((m) => m.kind === ts.SyntaxKind.DeclareKeyword);
  for (const node of source.statements) {
    if (ts.isInterfaceDeclaration(node) || ts.isTypeAliasDeclaration(node))
      continue;
    if (ts.isExportDeclaration(node)) {
      if (node.isTypeOnly) continue;
      if (node.exportClause && ts.isNamedExports(node.exportClause)) {
        for (const e of node.exportClause.elements)
          if (!e.isTypeOnly) names.add(e.name.text);
      } else if (!node.exportClause && node.moduleSpecifier) {
        // export * from "./x"
        const target = firstFile(
          resolve(dirname(file), node.moduleSpecifier.text)
        );
        if (target)
          exportsOf(target, coreDir, seen).forEach((n) => names.add(n));
      }
    } else if (ts.isExportAssignment(node)) {
      names.add("default");
    } else if (isExported(node)) {
      const isDefault = node.modifiers.some(
        (m) => m.kind === ts.SyntaxKind.DefaultKeyword
      );
      if (isDefault) names.add("default");
      else if (ts.isVariableStatement(node))
        node.declarationList.declarations.forEach(
          (d) => ts.isIdentifier(d.name) && names.add(d.name.text)
        );
      else if (node.name) names.add(node.name.text);
    }
  }
  return names;
}

/** The keys of PLUGIN_MODULES of the app (lib/plugin-runtime/modules.ts): what a plugin may import of it. */
function moduleKeys(coreDir) {
  const file = join(coreDir, "projectforge-next/lib/plugin-runtime/modules.ts");
  const text = readFileSync(file, "utf8");
  // `"components/shared/page-shell": pageShell,`, and `zod: zod,` where prettier unquotes a key that is an identifier.
  return new Set([...text.matchAll(/^ {2}"?([^":\s]+)"?:/gm)].map((m) => m[1]));
}

/**
 * Imports of modules of the app (`@/components/shared/page-shell`, `next/navigation`, `zod`, ...) are mapped to the
 * instances of the app (the bridge, `modules`), as far as the plugin has no file of its own at that path (`srcDir`, the
 * plugin's own `@/`). What the app doesn't list in PLUGIN_MODULES cannot be imported: the build says so.
 * @param {{ srcDir: string, coreDir: string }} options
 */
export function hostModules({ srcDir, coreDir }) {
  const keys = moduleKeys(coreDir);
  const appRoot = join(coreDir, "projectforge-next");
  const names = (key) => {
    const file = firstFile(join(appRoot, key));
    return file
      ? [...exportsOf(file, coreDir)]
      : Object.keys(requireFromApp(coreDir)(key)).filter(
          (k) => /^[A-Za-z_$][\w$]*$/.test(k) && k !== "__esModule"
        );
  };
  return {
    name: "host-modules",
    enforce: "pre",
    resolveId(id, importer) {
      if (id.startsWith("@/")) {
        const own = firstFile(join(srcDir, id.slice(2)));
        if (own) return own;
        if (keys.has(id.slice(2))) return `\0hostmod:${id.slice(2)}`;
        throw new Error(
          `${id} (imported by ${importer}) is neither a file of the plugin nor a module the app offers to plugins (lib/plugin-runtime/modules.ts).`
        );
      }
      return keys.has(id) ? `\0hostmod:${id}` : null;
    },
    load(id) {
      if (!id.startsWith("\0hostmod:")) return null;
      const key = id.slice("\0hostmod:".length);
      const lines = [`const m = ${BRIDGE}.modules[${JSON.stringify(key)}];`];
      names(key).forEach((n, i) => {
        // `export { x as null }`: a package may export names that are reserved words (zod: null, undefined, void, ...).
        if (n === "default") lines.push("export default m.default;");
        else
          lines.push(
            `const v${i} = m[${JSON.stringify(n)}]; export { v${i} as ${n} };`
          );
      });
      return lines.join("\n");
    },
  };
}

/**
 * The vite config of a plugin: one ES module `plugin.js` in `outDir`, no React in it.
 * @param {{ entry?: string, outDir: string, coreDir?: string, srcDir?: string }} options
 * `coreDir` is the ProjectForge checkout (the names the app exports and the modules it offers, see [hostModules]).
 */
export function pluginViteConfig({
  entry = "src/index.tsx",
  outDir,
  coreDir,
  srcDir = resolve(process.cwd(), "src"),
}) {
  if (!coreDir)
    throw new Error(
      "pluginViteConfig needs coreDir, the ProjectForge checkout."
    );
  return {
    plugins: [hostBridge({ coreDir }), hostModules({ srcDir, coreDir })],
    esbuild: { jsx: "automatic" },
    define: { "process.env.NODE_ENV": JSON.stringify("production") },
    build: {
      target: "es2022",
      outDir,
      emptyOutDir: true,
      lib: { entry, formats: ["es"], fileName: () => "plugin.js" },
      rollupOptions: {
        onwarn(warning, warn) {
          // The pages of the app say "use client", which means nothing to a bundle that is loaded by the client anyway.
          if (
            warning.code === "MODULE_LEVEL_DIRECTIVE" ||
            warning.code === "SOURCEMAP_ERROR"
          )
            return;
          warn(warning);
        },
      },
    },
  };
}

/** Fails the build if the bundle carries an implementation of React of its own (it must use the app's). */
export function checkBundle(file) {
  const code = readFileSync(file, "utf8");
  // `react.transitional.element` is no marker: react-is and the like name the symbol of an element to recognize one.
  const markers = [
    "__SECRET_INTERNALS_DO_NOT_USE",
    "ReactCurrentDispatcher",
    "ReactCurrentOwner",
  ];
  const found = markers.filter((m) => code.includes(m));
  if (found.length > 0) {
    throw new Error(`${file} bundles React (found ${found.join(", ")}).`);
  }
  console.log(`${file}: ${code.length} bytes, no bundled React`);
}
