/**
 * Runs the e2e suite in two phases, and merges both into one HTML report (playwright-report/):
 *
 * 1. Everything that may share the backend with others — the `parallel` project and the lanes (see
 *    the tags in playwright.config.ts) — side by side.
 * 2. The `isolated` project: specs that change what every other test would see, and untagged ones.
 *    One at a time, with nothing else running.
 *
 * Two runs rather than a project dependency: a failed dependency makes Playwright skip the projects
 * depending on it, so a single red test of phase 1 would have hidden the whole of phase 2.
 *
 *   npm run e2e                     # the whole suite, against Spring on :8080
 *   npm run e2e -- book-edit        # any Playwright arguments, passed to both phases
 *   npm run e2e:dev -- book-edit    # against the Next dev server on :3000 (same as --dev)
 *   npm run e2e -- --port 3001 …    # against a dev server on another port (another worktree's)
 *
 * The dev server targets save the rebuild and restart Spring would need after an edit — for quick
 * runs during development. A full run is steadier against Spring: the dev server compiles routes as
 * the tests reach them.
 *
 * Exits non-zero if either phase failed.
 */
import { spawnSync } from "node:child_process";
import { existsSync, rmSync } from "node:fs";
import path from "node:path";

const root = path.resolve(import.meta.dirname, "../..");
const blobDir = path.join(root, "blob-report");
const { args, devServerPort } = parseArgs(process.argv.slice(2));
const devEnv = devServerPort
  ? { E2E_BASE_URL: `http://localhost:${devServerPort}`, E2E_DEV_SERVER: "1" }
  : {};

/** Takes `--dev` and `--port <n>` (or `--port=<n>`) out; everything else goes to Playwright. */
function parseArgs(argv) {
  const rest = [];
  let port;
  for (let i = 0; i < argv.length; i++) {
    const arg = argv[i];
    if (arg === "--dev") port ??= "3000";
    else if (arg === "--port") port = argv[++i];
    else if (arg.startsWith("--port=")) port = arg.slice("--port=".length);
    else rest.push(arg);
  }
  if (port !== undefined && !/^\d+$/.test(port)) {
    console.error(`--port needs a port number, got "${port}".`);
    process.exit(2);
  }
  return { args: rest, devServerPort: port };
}

rmSync(blobDir, { recursive: true, force: true });

function playwright(argv, env = {}) {
  return spawnSync("npx", ["playwright", ...argv], {
    cwd: root,
    stdio: "inherit",
    env: { ...process.env, ...devEnv, ...env },
  }).status;
}

function phase(name, projects) {
  const blob = path.join(blobDir, `${name}.zip`);
  const status = playwright(
    [
      "test",
      ...projects.map((project) => `--project=${project}`),
      "--reporter=list,blob",
      // A filter given on the command line may match nothing in one of the phases.
      "--pass-with-no-tests",
      ...args,
    ],
    { PLAYWRIGHT_BLOB_OUTPUT_FILE: blob }
  );
  return { status, ran: existsSync(blob) };
}

const shared = phase("shared", ["parallel", "lane-*"]);
// No blob means no test ran at all — the global setup refused (a stale build, no server). The second
// phase would only repeat that refusal.
if (!shared.ran) process.exit(shared.status ?? 1);
const isolated = phase("isolated", ["isolated"]);

playwright(["merge-reports", "--reporter=html", blobDir], {
  PLAYWRIGHT_HTML_OPEN: "never",
});

process.exit(shared.status || isolated.status || 0);
