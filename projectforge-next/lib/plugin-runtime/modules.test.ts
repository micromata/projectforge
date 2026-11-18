import { existsSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";
import { PLUGIN_MODULES } from "./modules";

const ROOT = join(__dirname, "../..");

describe("PLUGIN_MODULES", () => {
  it("names modules of the app by path or package, each with exports", () => {
    for (const [key, module] of Object.entries(PLUGIN_MODULES)) {
      expect(Object.keys(module).length, key).toBeGreaterThan(0);
      const isPath = /^(components|hooks|lib|store|i18n)\//.test(key);
      if (isPath) {
        const found = [".ts", ".tsx", "/index.ts", "/index.tsx"].some((ext) =>
          existsSync(join(ROOT, key + ext))
        );
        expect(found, `${key} exists below projectforge-next`).toBe(true);
      }
    }
  });

  it("is the list the sdk promises: changing it is a decision (and PLUGIN_SDK_VERSION)", () => {
    expect(Object.keys(PLUGIN_MODULES).sort()).toEqual([
      "@tanstack/react-table",
      "components/data-table",
      "components/data-table/filter-pill-shell",
      "components/data-table/use-keyboard-nav",
      "components/features/chat/chat-panel",
      "components/shared/attachments/attachment-drop-area",
      "components/shared/chart/chart-value-tooltip",
      "components/shared/confirm-dialog",
      "components/shared/copy-button",
      "components/shared/copyable-collapsible-trigger",
      "components/shared/copyable-select",
      "components/shared/download-button",
      "components/shared/edit-page-tabs",
      "components/shared/edit/entity-edit-page",
      "components/shared/entity-autocomplete",
      "components/shared/entity-multi-autocomplete",
      "components/shared/form-alert",
      "components/shared/form/field-hint",
      "components/shared/form/form-context",
      "components/shared/guarded-dialog",
      "components/shared/hint-tooltip",
      "components/shared/jobs/job-progress",
      "components/shared/kpi-tile",
      "components/shared/list/entity-list-page",
      "components/shared/list/search-input",
      "components/shared/page-shell",
      "components/shared/page-title-row",
      "components/shared/rich-text",
      "components/shared/rich-text-editor",
      "components/shared/rights-hint",
      "components/shared/section-card",
      "components/shared/section-nav",
      "components/shared/spinner",
      "components/shared/statistics-line-class",
      "components/shared/status-pill",
      "components/ui/badge",
      "components/ui/button",
      "components/ui/chart",
      "components/ui/checkbox",
      "components/ui/collapsible",
      "components/ui/dialog",
      "components/ui/dropdown-menu",
      "components/ui/input",
      "components/ui/label",
      "components/ui/progress",
      "components/ui/select",
      "components/ui/tabs",
      "components/ui/textarea",
      "components/ui/toggle-group",
      "hooks/use-coarse-pointer",
      "hooks/use-current-user-ref",
      "hooks/use-format",
      "hooks/use-route-params",
      "hooks/use-tab-param",
      "i18n/config",
      "lib/base64url",
      "lib/charts/roles",
      "lib/docs-links",
      "lib/dynamic/grid/row-class",
      "lib/format",
      "lib/leaf-key",
      "lib/markdown-to-html",
      "lib/page-def/define-page",
      "lib/rs/chat",
      "lib/rs/client",
      "lib/rs/download",
      "lib/rs/jobs",
      "lib/rs/upload",
      "lib/toast",
      "lib/user-zone",
      "lib/utils",
      "lib/validation/from-metadata",
      "next/link",
      "next/navigation",
      "store/job-store",
      "zod",
    ]);
  });
});
