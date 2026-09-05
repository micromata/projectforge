import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat } from "./fixtures/format";
import { listRows, waitForRows } from "./fixtures/list-table";
import type { Page } from "@playwright/test";

/**
 * Reproduction probe for the reported "a new order takes a while to show up in the list, and only a
 * different search filter brings it in" behaviour.
 *
 * Three server paths are compared right after an insert, so the layer that lags is named rather than
 * guessed (see AbstractPagesRestUtils.getListPage, ListPageCache, DBQueryBuilder full-text mode):
 *
 *  A) POST /rs/order/list      {searchString}                 — non-paged, no ListPageCache, full-text
 *  B) POST /rs/order/listPage  {searchString, refresh:false}  — the path the order book UI uses
 *  C) POST /rs/order/listPage  {searchString, refresh:true}   — same, but ListPageCache bypassed
 *
 * The cache under B is *warmed before the insert* with the very same fingerprint (an empty result),
 * because that is the user's situation: the filter was already applied when the order was created.
 *
 *  - A slow, C slow      → the full-text (Lucene) index lags behind the commit.
 *  - A fast, B slow, C fast → the ListPageCache served a stale (warmed) id list; the change counter
 *                             did not invalidate it.
 *  - all fast            → no reproduction at the API level (look at the client React Query cache).
 */

const MARKER = "zzcacherepro"; // one lowercase word: survives tokenisation, matched by full-text.
const FE = { "X-PF-Frontend": "next" } as const;

// A live backend plus a full form of a route to compile on first touch — allow well beyond the default.
test.describe.configure({ timeout: 120_000 });

test.describe("order list caching", () => {
  test("visibility of a freshly inserted order across list/listPage paths", async ({
    loggedInPage: page,
  }) => {
    // A unique, tokeniser-safe search term; the title carries it so a full-text search can match it.
    const token = `${MARKER}${Date.now()}`;
    let id: number | null = null;
    try {
      await page.request
        .get("/rs/order/filter/reset", { headers: FE })
        .catch(() => undefined);

      // Warm the caches with this run's fingerprint *before* the order exists — this is the crux: the
      // user already had the filter applied. Both must come back empty.
      expect(await searchList(page, token), "list is empty before insert").toBe(
        0
      );
      expect(
        await searchListPage(page, token, false),
        "listPage is empty before insert (this warms the cache under B)"
      ).toBe(0);

      id = await createOrder(page, token);
      const t0 = Date.now();

      const a = await pollUntilFound(() => searchList(page, token));
      const b = await pollUntilFound(() => searchListPage(page, token, false));
      const c = await pollUntilFound(() => searchListPage(page, token, true));

      const rel = (t: number | null) =>
        t == null ? "NOT FOUND" : `${t - t0}ms`;
      // The measurement is the deliverable — printed so the run itself answers which layer lags.
      console.log(
        `[order-cache-repro] token=${token} id=${id}\n` +
          `  A /list      (full-text, no cache): ${rel(a)}\n` +
          `  B /listPage  (refresh:false, warmed cache): ${rel(b)}\n` +
          `  C /listPage  (refresh:true, cache bypassed): ${rel(c)}`
      );

      // Sanity: the order is committed, so every path must find it eventually. Which of them was slow
      // is in the log above; this only asserts nothing is lost outright.
      expect(a, "A /list must eventually find the order").not.toBeNull();
      expect(
        b,
        "B /listPage refresh:false must eventually find it"
      ).not.toBeNull();
      expect(
        c,
        "C /listPage refresh:true must eventually find it"
      ).not.toBeNull();
    } finally {
      await removeOrder(page, id);
    }
  });

  /**
   * The fix, on the case that reproduced it: with a search filter applied, an order is created without
   * the list's React Query client being invalidated (a second tab, the legacy Wicket UI, another user —
   * any write the open tab never hears about). Before the fix the *same* filter never brought it in
   * (staleTime 60s, focus refetch off), and only a *changed* filter — a new query key — did.
   *
   * The list now sets `refetchOnWindowFocus: "always"` (useMagicFilterQuery), so returning to the tab
   * refetches regardless of staleTime. This test proves that: the order stays hidden while the tab is
   * away, and the *same* filter shows it the moment the tab regains focus — no filter change needed.
   */
  test("a list refetches its applied filter when its tab regains focus (the fix)", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    const token = `${MARKER}${Date.now()}`;
    const searchBox = page.getByPlaceholder(t("filter.searchList"));
    const tokenRows = listRows(page).filter({ hasText: token });
    let id: number | null = null;
    try {
      await page.request
        .get("/rs/order/filter/reset", { headers: FE })
        .catch(() => undefined);

      // Log every paged list call the browser makes, with its body — so a refetch after the insert is
      // visible with its `refresh` flag and search string rather than inferred.
      const t0 = Date.now();
      page.on("request", (req) => {
        if (req.url().includes("/rs/order/listPage")) {
          console.log(
            `[req +${Date.now() - t0}ms] listPage ${req.postData() ?? ""}`
          );
        }
      });

      await goto(page, "/order");
      await waitForRows(page, 60_000);

      // Apply the search — nothing matches yet. The search box debounces, so waiting on skeletons or
      // on tokenRows==0 is not enough: those pass trivially before the *filtered* request ever fires
      // (the unfiltered list simply has no token row). Wait for the concrete listPage response that
      // carries the token — only then is the empty result truly resolved and cached, and only then is
      // the precondition ("the filter was applied before the order was created") actually established.
      const emptyFilteredResponse = page.waitForResponse(
        (res) =>
          res.url().includes("/rs/order/listPage") &&
          (res.request().postData() ?? "").includes(token),
        { timeout: 30_000 }
      );
      await searchBox.fill(token);
      await emptyFilteredResponse;
      await expect(page.locator("[data-slot=skeleton]")).toHaveCount(0, {
        timeout: 30_000,
      });
      await expect(
        tokenRows,
        "nothing matches the token before the insert"
      ).toHaveCount(0);

      // Create a matching order through the API — same session cookie, but this does not run the
      // client save mutation, so the browser's React Query cache is never invalidated. Exactly what a
      // second tab or the old UI does.
      console.log(`[insert +${Date.now() - t0}ms] creating order`);
      id = await createOrder(page, token);
      console.log(`[insert done +${Date.now() - t0}ms] id=${id}`);

      // Tab still focused, same filter: the cache is authoritative until something tells it otherwise,
      // so the new order is not yet in view. (This is the pre-fix steady state; the fix acts on focus,
      // below, not here.)
      await page.waitForTimeout(2_000);
      await expect(
        tokenRows,
        "while the tab stays put, the applied filter still shows the cached (empty) result"
      ).toHaveCount(0);

      // Leave the tab and come back: React Query's focus manager listens on `visibilitychange` and
      // treats a non-hidden document as focused, so hiding then re-showing is exactly a tab return. The
      // list is `refetchOnWindowFocus: "always"`, so this refetches regardless of staleTime.
      console.log(`[focus +${Date.now() - t0}ms] simulating tab hide + return`);
      await page.evaluate(() => {
        Object.defineProperty(document, "visibilityState", {
          configurable: true,
          get: () => "hidden",
        });
        window.dispatchEvent(new Event("visibilitychange"));
        Object.defineProperty(document, "visibilityState", {
          configurable: true,
          get: () => "visible",
        });
        window.dispatchEvent(new Event("visibilitychange"));
      });

      // The verdict: the *same* filter now shows the order — the reported "the old filter should also
      // trigger" is what the fix delivers, without touching the filter.
      await expect(
        tokenRows.first(),
        "on tab return the same filter refetches and shows the new order"
      ).toBeVisible({ timeout: 30_000 });
    } finally {
      await removeOrder(page, id);
    }
  });

  /**
   * The faithful in-app flow the user actually described ("wenn ich ein Angebot lege" — on /next/order,
   * not a second tab): the list is open with a search filter applied, the order is then created through
   * the real edit form, whose save runs `useSaveEntity` → `invalidateEntity` → `invalidateQueries(["order"])`.
   * On success the page navigates back to /order (EntityEditPage.afterSave → router.push).
   *
   * If the shared invalidation reaches the list, the returned list — with the same filter still applied —
   * shows the new order at once. If it does not (or the filter is not re-applied, or the remount serves a
   * still-fresh cache), the order is missing under the same filter: that is the reported bug, in-app.
   */
  test("a saved order shows up under the applied filter after returning to the list (in-app)", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { t } = format;
    const token = `${MARKER}${Date.now()}`;
    const searchBox = page.getByPlaceholder(t("filter.searchList"));
    const tokenRows = listRows(page).filter({ hasText: token });
    let id: number | null = null;
    try {
      await page.request
        .get("/rs/order/filter/reset", { headers: FE })
        .catch(() => undefined);

      const t0 = Date.now();
      page.on("request", (req) => {
        if (req.url().includes("/rs/order/listPage")) {
          console.log(
            `[req +${Date.now() - t0}ms] listPage ${req.postData() ?? ""}`
          );
        }
      });

      // 1) Open the list and apply the search — wait for the *filtered* response, so the empty result is
      // truly resolved and cached and the server stores this searchString as the current filter.
      await goto(page, "/order");
      await waitForRows(page, 60_000);
      const emptyFilteredResponse = page.waitForResponse(
        (res) =>
          res.url().includes("/rs/order/listPage") &&
          (res.request().postData() ?? "").includes(token),
        { timeout: 30_000 }
      );
      await searchBox.fill(token);
      await emptyFilteredResponse;
      await expect(tokenRows, "nothing matches the token yet").toHaveCount(0);

      // 2) Create a matching order through the real form (title carries the token so the filter matches).
      console.log(`[new +${Date.now() - t0}ms] navigating to /order/new`);
      await goto(page, "/order/new");
      const titel = page.getByLabel(label(format, "fibu.auftrag.title"), {
        exact: true,
      });
      await expect(
        titel,
        "the form has to hydrate before it is filled"
      ).toBeVisible({
        timeout: 60_000,
      });
      await titel.fill(`${token} order`);
      await page
        .getByLabel(label(format, "fibu.periodOfPerformance.from"), {
          exact: true,
        })
        .fill(format.date(new Date(2026, 2, 1)));
      await page
        .getByLabel(label(format, "fibu.periodOfPerformance.to"), {
          exact: true,
        })
        .fill(format.date(new Date(2026, 5, 30)));
      await page
        .getByRole("button", { name: t("fibu.auftrag.tooltip.addPosition") })
        .click();
      const posRow = page
        .locator("section", {
          has: page.getByText(t("fibu.auftrag.positions"), { exact: true }),
        })
        .locator('[data-slot="collapsible"]')
        .first();
      await posRow
        .getByLabel(label(format, "fibu.auftrag.nettoSumme"), { exact: true })
        .fill("1000");
      await posRow
        .getByLabel(label(format, "projectmanagement.personDays"), {
          exact: true,
        })
        .fill("3");
      await posRow
        .getByLabel(label(format, "projectmanagement.personDays"), {
          exact: true,
        })
        .blur();

      console.log(`[save +${Date.now() - t0}ms] clicking save`);
      await page.getByRole("button", { name: t("save"), exact: true }).click();

      // 3) A successful save navigates back to the list (EntityEditPage.afterSave).
      await expect(page).toHaveURL(/\/order$/, { timeout: 30_000 });
      console.log(`[back +${Date.now() - t0}ms] on the list again`);
      id = await currentOrderId(page, token);

      // 4) The verdict: with the same filter still applied, the freshly saved order must be visible.
      await expect(
        searchBox,
        "the applied search filter should survive the round trip"
      ).toHaveValue(token);
      await expect(
        tokenRows.first(),
        "in-app: the saved order must appear under the applied filter after the save invalidation"
      ).toBeVisible({ timeout: 10_000 });
    } finally {
      await removeOrder(page, id);
    }
  });
});

/** Finds the id of the order whose title carries the token, via the API (for cleanup). */
async function currentOrderId(
  page: Page,
  token: string
): Promise<number | null> {
  const res = await page.request.post("/rs/order/list", {
    headers: await writeHeaders(page),
    data: { searchString: token },
  });
  const body = (await res.json()) as {
    resultSet?: { id?: number; titel?: string }[];
  };
  return body.resultSet?.find((r) => r.titel?.includes(token))?.id ?? null;
}

/** Creates a minimal order whose title carries the search token; returns its id. */
async function createOrder(page: Page, token: string): Promise<number> {
  const res = await page.request.put("/rs/order/saveorupdate", {
    headers: await writeHeaders(page),
    data: {
      data: {
        titel: `${token} order`,
        status: "IN_ERSTELLUNG",
        periodOfPerformanceBegin: "2026-03-01",
        periodOfPerformanceEnd: "2026-06-30",
        positionen: [{ number: 1, status: "IN_ERSTELLUNG", titel: token }],
      },
    },
  });
  const body = (await res.json()) as {
    variables?: { id?: number };
    validationErrors?: { message?: string }[];
  };
  if (!res.ok() || body.variables?.id == null) {
    const reason =
      body.validationErrors?.map((e) => e.message).join("; ") ??
      `HTTP ${res.status()}`;
    throw new Error(`Could not create the order: ${reason}`);
  }
  return body.variables.id;
}

/** Count of hits for `token` via the non-paged full-text list. */
async function searchList(page: Page, token: string): Promise<number> {
  const res = await page.request.post("/rs/order/list", {
    headers: await writeHeaders(page),
    data: { searchString: token },
  });
  const body = (await res.json()) as { resultSet?: { titel?: string }[] };
  return countMatches(body.resultSet, token);
}

/** Count of hits for `token` via the paged path, with or without the ListPageCache. */
async function searchListPage(
  page: Page,
  token: string,
  refresh: boolean
): Promise<number> {
  const res = await page.request.post("/rs/order/listPage", {
    headers: await writeHeaders(page),
    data: {
      filter: { searchString: token },
      offset: 0,
      limit: 50,
      refresh,
      // Never remember this probe's filter as the user's current one.
      doNotStore: true,
    },
  });
  const body = (await res.json()) as { resultSet?: { titel?: string }[] };
  return countMatches(body.resultSet, token);
}

/** How many returned rows actually carry the token — the query may widen, the assertion must not. */
function countMatches(
  rows: { titel?: string }[] | undefined,
  token: string
): number {
  return (rows ?? []).filter((r) => r.titel?.includes(token)).length;
}

/**
 * Calls `probe` until it reports a hit, and answers the wall-clock time of the first hit (or null if
 * none came within the budget). Short interval, generous ceiling: the lag under test is seconds, and a
 * miss must be a real miss, not an early give-up.
 */
async function pollUntilFound(
  probe: () => Promise<number>
): Promise<number | null> {
  const deadline = Date.now() + 30_000;
  for (;;) {
    if ((await probe()) > 0) return Date.now();
    if (Date.now() >= deadline) return null;
    await new Promise((r) => setTimeout(r, 300));
  }
}

/** Marks the order as deleted, so the run leaves nothing the list shows (physical delete is refused). */
async function removeOrder(page: Page, id: number | null) {
  if (id == null) return;
  const read = await page.request.get(`/rs/order/${id}`, { headers: FE });
  const order = await read.json();
  await page.request
    .delete("/rs/order/markAsDeleted", {
      headers: await writeHeaders(page),
      data: { data: order },
    })
    .catch(() => undefined);
}

/** The headers every state changing call needs — the CSRF token is read per call, not cached. */
async function writeHeaders(page: Page): Promise<Record<string, string>> {
  const status = await page.request.get("/rs/userStatus", { headers: FE });
  const { csrfToken } = (await status.json()) as { csrfToken: string };
  return {
    ...FE,
    "X-PF-CSRF-Token": csrfToken,
    "Content-Type": "application/json",
  };
}
