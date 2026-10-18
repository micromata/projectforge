import type { APIRequestContext } from "@playwright/test";
import type { NumberRangesInfo } from "../../lib/rs/number-ranges";
import { fetchEntity, insert, writeHeaders } from "./seed";

/**
 * The number ranges of customers and projects (`/rs/numberRanges`), and the switch that decides between
 * two of their modes.
 *
 * Three modes (`NumberRangeService.numberRanges`):
 *
 * - **configured**: ranges given by `projectforge.fibu.number-ranges.*` — bound at startup, so only a
 *   slot configured that way (and restarted) runs these tests (see number-ranges.spec.ts);
 * - **legacy**: no ranges configured, but cost units (`fibu.costConfigured`): the former ranges 4
 *   (internal) and 5 (customers 0-999);
 * - **free**: neither; customer and project numbers are free.
 *
 * The last two are one configuration parameter apart, which changes at runtime: [setCostConfigured].
 * It is global state — every other test sees it —, so a spec using it runs isolated and restores it.
 */

/** The number ranges as the forms get them. */
export async function fetchNumberRanges(
  request: APIRequestContext
): Promise<NumberRangesInfo> {
  const res = await request.get("/rs/numberRanges", {
    headers: { "X-PF-Frontend": "next" },
  });
  if (!res.ok()) {
    throw new Error(`Could not read the number ranges: HTTP ${res.status()}`);
  }
  return (await res.json()) as NumberRangesInfo;
}

/** The key of `ConfigurationParam.COST_CONFIGURED`. */
const COST_CONFIGURED = "fibu.costConfigured";

interface StoredConfiguration {
  id: number;
  parameter?: string;
  booleanValue?: boolean | null;
}

/** The row of `fibu.costConfigured`, read through the configuration page (`ConfigurationEntityRest`). */
async function costConfiguredEntry(
  request: APIRequestContext
): Promise<StoredConfiguration> {
  const res = await request.post("/rs/configuration/list", {
    headers: await writeHeaders(request),
    data: {},
  });
  if (!res.ok()) {
    throw new Error(`Could not list the configuration: HTTP ${res.status()}`);
  }
  const body = (await res.json()) as { resultSet?: StoredConfiguration[] };
  const entry = body.resultSet?.find(
    (row) => row.parameter === COST_CONFIGURED
  );
  if (entry == null) {
    throw new Error(`No configuration parameter ${COST_CONFIGURED}.`);
  }
  return entry;
}

/** Whether cost units are configured (`fibu.costConfigured`). */
export async function isCostConfigured(
  request: APIRequestContext
): Promise<boolean> {
  return (await costConfiguredEntry(request)).booleanValue === true;
}

/**
 * Switches cost units on or off. Takes effect at once (`ConfigurationDao.afterInsertOrModify` expires the
 * configuration cache); a page already showing a form has to be loaded again, though: the frontend reads
 * the number ranges once per session (`useNumberRanges`, `staleTime: Infinity`).
 */
export async function setCostConfigured(
  request: APIRequestContext,
  on: boolean
): Promise<void> {
  const { id } = await costConfiguredEntry(request);
  const stored = await fetchEntity<Record<string, unknown>>(
    request,
    "configuration",
    id
  );
  if (stored.booleanValue === on) return;
  await insert(request, "configuration", { ...stored, booleanValue: on });
  const info = await fetchNumberRanges(request);
  if (info.costConfigured !== on) {
    throw new Error(`${COST_CONFIGURED} is still ${!on} after setting it.`);
  }
}

/**
 * Whether number ranges are configured (`projectforge.fibu.number-ranges.*`), as opposed to the legacy
 * ranges 4/5, which apply only with cost units: asked with cost units off, and switched back.
 */
export async function hasConfiguredRanges(
  request: APIRequestContext
): Promise<boolean> {
  const before = await isCostConfigured(request);
  try {
    await setCostConfigured(request, false);
    return (await fetchNumberRanges(request)).ranges.length > 0;
  } finally {
    await setCostConfigured(request, before);
  }
}
