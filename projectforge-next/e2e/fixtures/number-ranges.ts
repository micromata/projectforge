import type { APIRequestContext } from "@playwright/test";
import type {
  NextFreeProjectNumber,
  NumberRangesInfo,
} from "../../lib/rs/number-ranges";
import { fetchEntity, insert, writeHeaders } from "./seed";

/**
 * The number ranges of customers and projects (`/rs/numberRanges`), and the cost units switch.
 *
 * Three modes (`NumberRangeService.mode`, `NumberRangesInfo.mode`), fixed while the server runs:
 *
 * - **CONFIGURED**: ranges given by `projectforge.fibu.number-ranges.*` — bound at startup;
 * - **LEGACY**: no ranges configured, but Kost2 of projects in the former ranges 4 (internal) and 5
 *   (customers 0-999) exist — an installation that used them, e.g. a copy of Micromata's;
 * - **FREE**: neither (e.g. a fresh database); customer and project numbers are free.
 *
 * Cost units (`fibu.costConfigured`, [setCostConfigured]) don't change the mode, only whether projects offer
 * cost 2 types. It is global state — every other test sees it —, so a spec switching it runs isolated and
 * restores it.
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

/** The next free project number of the customer's range, as the project form offers it; null without range. */
export async function fetchNextFreeProjectNumber(
  request: APIRequestContext,
  customerId: number
): Promise<NextFreeProjectNumber | null> {
  const res = await request.get(
    `/rs/numberRanges/nextFreeProjectNumber?customerId=${customerId}`,
    { headers: { "X-PF-Frontend": "next" } }
  );
  if (!res.ok()) {
    throw new Error(
      `Could not read the next free project number: HTTP ${res.status()}`
    );
  }
  const text = await res.text();
  return text ? (JSON.parse(text) as NextFreeProjectNumber) : null;
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
