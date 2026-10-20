import type { Page } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { writeHeaders } from "./fixtures/seed";
import { label, userFormat } from "./fixtures/format";
import { listRows } from "./fixtures/list-table";
import { SKILL_ENTRY_METADATA } from "../lib/metadata/skill-entry.generated";
import { SKILL_ENTRY_PAGE } from "../components/features/skill-entry/skill-entry.page";
import { columnHeaderKeyOf, columnIdOf } from "../lib/page-def/define-page";

/**
 * The skill matrix against the live backend (see SKILL_ENTRY_PAGE and SkillEntryEntityRest).
 *
 * The database may be a copy of production: nothing is written. The save test intercepts the save request,
 * the mass update test the update request (what it writes is only the selection in the session, dropped
 * again at its end).
 */
test.describe("skillentry", { tag: "@parallel" }, () => {
  test("shows the declared columns under the labels of SkillEntryDO", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/skillentry");

    await expect(
      page.getByRole("heading", {
        name: format.t(SKILL_ENTRY_PAGE.titleKey),
      })
    ).toBeVisible();
    for (const column of SKILL_ENTRY_PAGE.columns) {
      if (column.hiddenByDefault) continue;
      const key = columnHeaderKeyOf(column, SKILL_ENTRY_METADATA);
      await expect(
        page.getByRole("columnheader", {
          name: new RegExp(`^${escape(label(format, key))}`),
        }),
        `column ${columnIdOf(column)}`
      ).toHaveCount(1);
    }
  });

  test("posts the skill with the owner and the stars on save", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    let posted: Record<string, unknown> | undefined;
    await page.route("**/rs/skillentry/{insert,update}*", (route) => {
      posted = route.request().postDataJSON() as Record<string, unknown>;
      return route.abort();
    });

    await goto(page, "/skillentry/new");
    // The owner is the logged-in user, preset by the backend and not editable.
    await expect(
      page.getByText(label(format, "plugins.skillmatrix.owner"))
    ).toBeVisible();
    await page
      .getByRole("combobox", {
        name: label(format, "plugins.skillmatrix.skill"),
      })
      .or(
        page.getByRole("textbox", {
          name: label(format, "plugins.skillmatrix.skill"),
        })
      )
      .fill("e2e skill");
    const rating = page.getByRole("radiogroup", {
      name: label(format, "plugins.skillmatrix.rating"),
    });
    await rating
      .getByRole("radio", {
        name: format.t("plugins.skillmatrix.rating.2"),
        exact: true,
      })
      .click();
    await expect(
      page.getByText(format.t("plugins.skillmatrix.rating.2"), { exact: true })
    ).toBeVisible();
    const interest = page.getByRole("radiogroup", {
      name: label(format, "plugins.skillmatrix.interest"),
    });
    // A second click on the star of the current level clears the rating.
    const crazy = interest.getByRole("radio", {
      name: format.t("plugins.skillmatrix.interest.3"),
      exact: true,
    });
    await crazy.click();
    await crazy.click();
    await interest
      .getByRole("radio", {
        name: format.t("plugins.skillmatrix.interest.1"),
        exact: true,
      })
      .click();
    await page.getByRole("button", { name: format.t("create") }).click();

    await expect
      .poll(() => posted, { message: "the save reaches the backend" })
      .toBeDefined();
    const body = (posted?.data ?? posted) as Record<string, unknown>;
    expect(body.skill).toBe("e2e skill");
    expect(body.rating).toBe(2);
    expect(body.interest).toBe(1);
    expect((body.owner as { id?: number } | undefined)?.id).toBeDefined();
  });

  test("exports the statistics as Excel", async ({ loggedInPage: page }) => {
    const format = await userFormat(page);
    await goto(page, "/skillentry");
    await page
      .getByRole("button", { name: format.t("export"), exact: true })
      .click();
    const download = page.waitForEvent("download");
    await page
      .getByRole("menuitem", {
        name: new RegExp(`^${escape(format.t("exportAsXls"))}`),
      })
      .click();
    // Named by the backend through Content-Disposition (SkillMatrixServicesRest).
    expect((await download).suggestedFilename()).toMatch(
      /^SkillMatrix_.*\.xlsx$/
    );
  });

  test("mass updates the levels by their texts", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const massUpdate = SKILL_ENTRY_PAGE.massUpdate!;
    // A selection another run left behind would restore itself into this one.
    await cancelSelection(page);
    await goto(page, "/skillentry");
    const firstRow = listRows(page).first();
    const hasRows = await firstRow
      .waitFor({ timeout: 30_000 })
      .then(() => true)
      .catch(() => false);
    test.skip(!hasRows, "The logged-in user has no skills to select.");

    try {
      await page
        .getByRole("button", { name: format.t("multiselection.button") })
        .and(page.locator("[aria-pressed]"))
        .click();
      const ticked = page.waitForResponse(
        (response) =>
          response.url().endsWith(`/rs/${massUpdate.endpoint}/select`) &&
          (response.request().postDataJSON() as { selectedIds?: unknown[] })
            ?.selectedIds?.length === 1
      );
      await firstRow.getByRole("checkbox").check();
      await ticked;
      await page
        .getByRole("button", { name: format.t("massUpdate.button") })
        .click();
      await expect(page).toHaveURL(new RegExp(`${massUpdate.route}$`));

      // Rating and interest are chosen by their level texts, not typed as numbers.
      await page
        .getByRole("combobox", {
          name: label(format, "plugins.skillmatrix.rating"),
          exact: true,
        })
        .click();
      for (const level of [0, 1, 2]) {
        await expect(
          page.getByRole("option", {
            name: format.t(`plugins.skillmatrix.rating.${level}`),
            exact: true,
          })
        ).toBeVisible();
      }
      await page
        .getByRole("option", {
          name: format.t("plugins.skillmatrix.rating.3"),
          exact: true,
        })
        .click();
      await expect(
        page.getByRole("combobox", {
          name: label(format, "plugins.skillmatrix.interest"),
          exact: true,
        })
      ).toBeVisible();
      await expect(
        page.getByRole("textbox", { name: label(format, "comment") })
      ).toBeVisible();

      await page.route(`**/rs/${massUpdate.endpoint}/update`, (route) =>
        route.abort()
      );
      const posted = page.waitForRequest(
        (request) =>
          request.url().endsWith(`/rs/${massUpdate.endpoint}/update`) &&
          request.method() === "POST"
      );
      await page.getByRole("button", { name: format.t("save") }).click();
      // The preview is the real one (read-only): it names the level text, not the number.
      const dialog = page.getByRole("alertdialog");
      await expect(dialog).toContainText(
        format.t("plugins.skillmatrix.rating.3")
      );
      await dialog.getByRole("button", { name: format.t("save") }).click();
      expect((await posted).postDataJSON()).toEqual({
        rating: { textValue: "3" },
      });
    } finally {
      await cancelSelection(page);
    }
  });

  test("redirects the old React list", async ({ loggedInPage: page }) => {
    await page.goto("/react/skillentry");
    await expect(page).toHaveURL(/\/next\/skillentry$/);
  });
});

async function cancelSelection(page: Page): Promise<void> {
  await page.request
    .post(`/rs/${SKILL_ENTRY_PAGE.massUpdate!.endpoint}/cancel`, {
      headers: await writeHeaders(page.request),
    })
    .catch(() => undefined);
}

function escape(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
