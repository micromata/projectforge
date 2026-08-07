import { defineConfig, globalIgnores } from "eslint/config";
import nextVitals from "eslint-config-next/core-web-vitals";
import nextTs from "eslint-config-next/typescript";
import prettier from "eslint-config-prettier";

const eslintConfig = defineConfig([
  ...nextVitals,
  ...nextTs,
  prettier,
  // Override default ignores of eslint-config-next.
  globalIgnores([
    // Default ignores of eslint-config-next:
    ".next/**",
    "out/**",
    "build/**",
    "next-env.d.ts",
    // shadcn-managed sources (regenerated via `npx shadcn@latest add`):
    "components/ui/**",
    "hooks/use-mobile.ts",
    // Playwright's generated report: bundled third party code, not ours to lint.
    "playwright-report/**",
    "test-results/**",
  ]),
  {
    // Playwright fixtures are not React. Its `use()` callback — how a fixture hands its value to
    // the test — looks like the `use` hook to the React rules, which then demand a component.
    files: ["e2e/**"],
    rules: { "react-hooks/rules-of-hooks": "off" },
  },
]);

export default eslintConfig;
