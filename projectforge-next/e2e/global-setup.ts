import { existsSync, readdirSync, readFileSync, statSync } from "node:fs";
import { join, relative } from "node:path";
import type { FullConfig } from "@playwright/test";
import { hasRole, ROLES } from "./fixtures/credentials";
import { apiBaseUrl, createSession } from "./fixtures/session";

/**
 * Runs once before any test:
 *
 * 1. Against Spring (the default target), refuses to test a stale build. Spring serves the static
 *    export Gradle copied into the build resources, not the working tree, so after an edit a run
 *    would test yesterday's code — and pass or fail for reasons that have nothing to do with it.
 * 2. Logs every role the local instance has an account for in once, for `login()` to hand out (see
 *    fixtures/session.ts).
 */
export default async function globalSetup(config: FullConfig): Promise<void> {
  const baseURL = config.projects[0]?.use.baseURL ?? "";
  if (!isDevServer(baseURL) && !process.env.E2E_SKIP_BUILD_CHECK) {
    await checkBuildIsCurrent(baseURL);
  }
  for (const role of ROLES.filter(hasRole)) {
    await createSession(baseURL, role);
  }
}

/**
 * The Next dev server compiles the working tree itself, so there is nothing to be stale. Set by the
 * runner's `--port`/`--dev` (e2e/tools/run-e2e.mjs); :3000 is the dev server's default port.
 */
function isDevServer(baseURL: string): boolean {
  return process.env.E2E_DEV_SERVER === "1" || new URL(baseURL).port === "3000";
}

const ROOT = join(__dirname, "..");

/** What the export is built from; tests, docs and the e2e suite itself don't change it. */
const SOURCE_DIRS = [
  "app",
  "components",
  "hooks",
  "i18n",
  "lib",
  "messages",
  "store",
];
const SOURCE_FILES = [
  "next.config.ts",
  "package.json",
  "postcss.config.mjs",
  "tsconfig.json",
];

/**
 * The copy Spring serves from in development: `processResources` of projectforge-application copies
 * the export (via projectforge-next's `copyNextBuild`) into its build resources, which bootRun puts
 * on the classpath.
 */
const LOCAL_EXPORT = join(
  ROOT,
  "../projectforge-application/build/resources/main/static/next/index.html"
);

const REBUILD_HINT =
  `Rebuild it with\n\n    ./gradlew :projectforge-application:processResources\n\n` +
  `To test against the dev server instead, run npm run e2e:dev (or npm run e2e -- --port <n>); to run ` +
  `anyway, E2E_SKIP_BUILD_CHECK=1.`;

/**
 * Two questions: is this checkout's export older than its sources, and is it the export the server
 * actually serves? The second one fails when the build was copied but not picked up, and when the
 * server on the port belongs to another worktree — a run would then test that one's code.
 */
async function checkBuildIsCurrent(baseURL: string): Promise<void> {
  if (!existsSync(LOCAL_EXPORT)) {
    // Spring may run from a boot jar, whose export this check cannot see. Not a reason to refuse.
    console.warn(
      `[e2e] No exported build at ${relative(ROOT, LOCAL_EXPORT)}, so the build check is skipped.`
    );
    return;
  }
  const newer = newestSource();
  if (newer && newer.mtime > statSync(LOCAL_EXPORT).mtimeMs) {
    throw new Error(
      `The Next export is older than the sources (${newer.path} changed after the last ` +
        `build). ${REBUILD_HINT}`
    );
  }
  const local = buildId(readFileSync(LOCAL_EXPORT, "utf8"));
  const served = buildId(
    await (await fetch(new URL("/next/", apiBaseUrl(baseURL)))).text()
  );
  if (local && served && local !== served) {
    throw new Error(
      `${baseURL} serves another Next build (${served}) than this checkout holds (${local}): ` +
        `either the server runs from another worktree, or it has not picked up the last build ` +
        `yet (restart it). ${REBUILD_HINT}`
    );
  }
}

/** Next's build id, as the exported HTML carries it in its RSC payload (`"b":"…"`). */
function buildId(html: string): string | undefined {
  return /\\"b\\":\\"([\w-]+)/.exec(html)?.[1];
}

function newestSource(): { path: string; mtime: number } | undefined {
  let newest: { path: string; mtime: number } | undefined;
  const visit = (path: string) => {
    if (!existsSync(path)) return;
    const stat = statSync(path);
    if (stat.isDirectory()) {
      for (const entry of readdirSync(path)) visit(join(path, entry));
    } else if (!/\.(test|spec)\.tsx?$|\.md$/.test(path)) {
      if (!newest || stat.mtimeMs > newest.mtime) {
        newest = { path: relative(ROOT, path), mtime: stat.mtimeMs };
      }
    }
  };
  for (const entry of [...SOURCE_DIRS, ...SOURCE_FILES])
    visit(join(ROOT, entry));
  return newest;
}
