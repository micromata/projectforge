import type { Page } from "@playwright/test";
import {
  groupBaseName,
  suggestedGroupName,
  type GroupNomenclatureConfig,
  type GroupNomenclatureRole,
  type NomenclatureEntity,
} from "../../lib/group-nomenclature";

/**
 * The group name a wizard suggests for a role, following the installation's configured nomenclature
 * (`GET rs/group/nomenclature`) the way the page does: a configured suffix or letter case changes the
 * suggestion, so a spec can't assume the base name comes back as typed.
 *
 * The base is either a name as it is (the structure wizard's element outside of a project), or a project
 * and its customer, which go through the configured pattern first (the project wizard).
 */
export async function expectedGroupName(
  page: Page,
  role: GroupNomenclatureRole,
  base:
    | string
    | { project: NomenclatureEntity; customer?: NomenclatureEntity | null },
  defaultSuffix: string | null = null
): Promise<string> {
  const res = await page.request.get("/rs/group/nomenclature", {
    headers: { "X-PF-Frontend": "next" },
  });
  const config = res.ok()
    ? ((await res.json().catch(() => null)) as GroupNomenclatureConfig | null)
    : null;
  const baseName =
    typeof base === "string" ? base : groupBaseName(config, base);
  return suggestedGroupName(config, role, baseName, defaultSuffix) ?? "";
}
