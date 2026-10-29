import type { APIRequestContext } from "@playwright/test";
import { test, expect, goto, login } from "./fixtures/auth";
import { hasRole } from "./fixtures/credentials";
import { label, userFormat } from "./fixtures/format";
import { MARKER, uniqueSuffix, writeHeaders } from "./fixtures/seed";

/**
 * The Kost2 reallocation (`/kost2-reallocation`, `Kost2ReallocationRest`), finance only.
 *
 * The cases stop at the preview: applying a run moves time sheets and writes kost2 shares of real accounting
 * records, which no spec may do on a shared database. Each case works on a draft it created itself and deletes
 * it afterwards, whatever happened in between (a draft is deleted for good, an applied run couldn't be).
 *
 * Run as `finance-user` (the finance group without the admin group) and `normalo-user` for the refusal. An
 * instance without these accounts, or without a project, skips rather than fails.
 */

const ROUTE = "/kost2-reallocation";
const RS = "/rs/kost2Reallocation";

test.describe.configure({ timeout: 120_000 });

/** A project to reallocate to: the first one of the list that isn't one of the tests' own. */
async function findProject(
  request: APIRequestContext
): Promise<{ id: number } | null> {
  const res = await request.post("/rs/project/list", {
    headers: await writeHeaders(request),
    data: {},
  });
  if (!res.ok()) return null;
  const body = (await res.json()) as {
    resultSet?: { id?: number; name?: string; status?: string }[];
  };
  const project = (body.resultSet ?? []).find(
    (row) =>
      row.id != null && row.status !== "ENDED" && !row.name?.startsWith(MARKER)
  );
  return project?.id != null ? { id: project.id } : null;
}

/** Saves a draft run through the API and answers with its id. */
async function createDraft(
  request: APIRequestContext,
  projektId: number,
  description: string
): Promise<number> {
  const res = await request.post(`${RS}/save`, {
    headers: await writeHeaders(request),
    data: {
      description,
      projekt: { id: projektId, displayName: "" },
      fromYear: 2000,
      fromMonth: 1,
      toYear: 2000,
      toMonth: 1,
      rules: [],
    },
  });
  expect(res.ok(), `save: HTTP ${res.status()}`).toBe(true);
  const run = (await res.json()) as { id: number };
  return run.id;
}

/** Deletes the draft, if it still exists (the UI case deletes it itself). */
async function removeDraft(request: APIRequestContext, runId: number) {
  await request.post(`${RS}/delete`, {
    headers: await writeHeaders(request),
    data: { runId },
  });
}

test.describe("kost2 reallocation", { tag: "@parallel" }, () => {
  test("refused for a user without the finance group", async ({ page }) => {
    test.skip(!hasRole("normalo-user"), "No normalo-user on this instance.");
    await login(page, "normalo-user");
    const list = await page.request.get(`${RS}/list`, {
      headers: { "X-PF-Frontend": "next" },
    });
    expect(list.status()).toBe(403);
    const save = await page.request.post(`${RS}/save`, {
      headers: await writeHeaders(page.request),
      data: { description: `${MARKER} refused`, rules: [] },
    });
    expect(save.status()).toBe(403);
  });

  test("a draft is previewed and deleted in the wizard", async ({ page }) => {
    test.skip(!hasRole("finance-user"), "No finance-user on this instance.");
    await login(page, "finance-user");
    const project = await findProject(page.request);
    test.skip(project == null, "No project on this instance.");
    const format = await userFormat(page);
    const description = `${MARKER} reallocation ${uniqueSuffix()}`;
    const runId = await createDraft(page.request, project!.id, description);
    try {
      await goto(page, `${ROUTE}/${runId}`);
      await expect(page.locator("#kr-description")).toHaveValue(description, {
        timeout: 60_000,
      });
      // A month without records: the preview has nothing to change.
      await page
        .getByRole("button", {
          name: label(format, "fibu.kost2Reallocation.preview"),
          exact: true,
        })
        .click();
      await expect(
        page.getByRole("heading", {
          name: `${label(format, "fibu.buchungssaetze")} (0)`,
        })
      ).toBeVisible();
      await expect(
        page.getByRole("heading", {
          name: `${label(format, "fibu.kost2Reallocation.timesheets")} (0)`,
        })
      ).toBeVisible();

      // Delete, confirmed in the dialog: back on the list, without the run.
      const deleteLabel = label(format, "delete");
      await page
        .getByRole("button", { name: deleteLabel, exact: true })
        .click();
      await page
        .getByRole("alertdialog")
        .getByRole("button", { name: deleteLabel, exact: true })
        .click();
      await expect(page).toHaveURL(/\/kost2-reallocation\/?$/);
      await expect(page.getByText(description, { exact: true })).toHaveCount(0);
      const runs = await page.request.get(`${RS}/list`, {
        headers: { "X-PF-Frontend": "next" },
      });
      const ids = ((await runs.json()) as { id: number }[]).map((r) => r.id);
      expect(ids).not.toContain(runId);
    } finally {
      await removeDraft(page.request, runId);
    }
  });

  test("saving a new draft without description is refused", async ({
    page,
  }) => {
    test.skip(!hasRole("finance-user"), "No finance-user on this instance.");
    await login(page, "finance-user", `${ROUTE}/new`);
    const format = await userFormat(page);
    await expect(page.locator("#kr-description")).toBeVisible({
      timeout: 60_000,
    });
    await page
      .getByRole("button", { name: label(format, "save"), exact: true })
      .click();
    await expect(page.locator('[data-tone="error"]')).toBeVisible();
    // Nothing was saved: still on the new draft.
    await expect(page).toHaveURL(/\/kost2-reallocation\/new\/?$/);
  });
});
