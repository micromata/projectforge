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
  {
    // Every toast goes through lib/toast.ts, which is where the app's defaults live (how long an error
    // stays, and that it can be closed). Imported from "sonner" directly, a call site silently opts out
    // of them — hence the rule rather than a note in the module. `Toaster` is the component and has no
    // defaults to miss, so app/layout.tsx keeps importing it from sonner.
    files: ["components/**", "lib/**", "hooks/**", "store/**", "app/**"],
    ignores: ["lib/toast.ts"],
    rules: {
      "no-restricted-imports": [
        "error",
        {
          paths: [
            {
              name: "sonner",
              importNames: ["toast"],
              message: 'Import { toast } from "@/lib/toast" instead.',
            },
          ],
        },
      ],
    },
  },
]);

export default eslintConfig;
