import type { Page } from "@playwright/test";
import { test, expect, goto, waitForHydration } from "./fixtures/auth";
import { translate, locales, type UserFormat } from "./fixtures/format";
import { deleteEntity, insert, MARKER, uniqueSuffix } from "./fixtures/seed";

/**
 * The external access of a data transfer area in next (`/next/public/datatransfer/<token>`): someone
 * without a ProjectForge login opens the area with its password. The legacy React page runs in
 * parallel and is not tested here.
 *
 * The plain `page` of the fixture is not logged in, which is the point; the areas are created through
 * `seedRequest` with a token and password of the test's own, and deleted again.
 */

const FILE = {
  mimeType: "text/plain",
  buffer: Buffer.from("ProjectForge e2e public data transfer\n"),
};

const PASSWORD = "pf-e2e-Secret";

/** 30 characters, as `DataTransferAreaDao.getAnonymousArea` demands. */
function newToken(): string {
  return `pfe2e${uniqueSuffix()}`
    .replace(/[^A-Za-z0-9]/g, "")
    .padEnd(30, "x")
    .slice(0, 30);
}

async function createArea(
  request: Parameters<typeof insert>[0],
  access: { download: boolean; upload: boolean }
) {
  const status = await request.get("/rs/userStatus", {
    headers: { "X-PF-Frontend": "next" },
  });
  const { userData } = (await status.json()) as {
    userData: { userId: number };
  };
  const areaName = `${MARKER} public datatransfer ${uniqueSuffix()}`;
  const token = newToken();
  const id = await insert(request, "datatransfer", {
    areaName,
    admins: [{ id: userData.userId }],
    expiryDays: 7,
    maxUploadSizeKB: 100 * 1024,
    externalDownloadEnabled: access.download,
    externalUploadEnabled: access.upload,
    externalAccessToken: token,
    externalPassword: PASSWORD,
  });
  return { id, areaName, token };
}

/**
 * No login, so no user locale: the page speaks the browser's language (LocaleFilter, and the client's
 * own resolution). The locale is read off the login button.
 */
async function publicFormat(page: Page): Promise<UserFormat["t"]> {
  const button = page.locator('button[type="submit"]');
  await expect(button).toBeVisible();
  const text = (await button.textContent())?.trim();
  return translate(
    locales().find((locale) => translate(locale)("login._") === text) ?? "de"
  );
}

async function login(page: Page, password: string) {
  await page.fill("#externalPassword", password);
  await page.locator('button[type="submit"]').click();
}

function storedRow(page: Page, t: UserFormat["t"], name: string) {
  return page.getByRole("link", {
    name: `${t("download._")}: ${name}`,
    exact: true,
  });
}

test.describe("public data transfer", { tag: "@parallel" }, () => {
  test("logs in with the area's password, uploads, renames and deletes an own file, logs out", async ({
    page,
    seedRequest,
  }) => {
    const area = await createArea(seedRequest, {
      download: true,
      upload: true,
    });
    let name = `pf-e2e-public-${uniqueSuffix()}.txt`;
    try {
      await goto(page, `/public/datatransfer/${area.token}`);
      await waitForHydration(page);
      const t = await publicFormat(page);
      // Before the login nothing of the area is shown, not even its name.
      await expect(page.getByText(area.areaName)).toHaveCount(0);

      await login(page, "definitely-not-the-password");
      await expect(
        page.locator('[role="alert"][data-tone="error"]')
      ).toBeVisible();

      // The failed attempt locks the token for a second (LoginProtection), so the right one is retried.
      await expect(async () => {
        await login(page, PASSWORD);
        await expect(
          page.getByRole("heading", { name: area.areaName })
        ).toBeVisible({
          timeout: 2_000,
        });
      }).toPass({ timeout: 15_000 });

      await page
        .getByLabel(t("file.upload.choose"))
        .setInputFiles({ name, ...FILE });
      await expect(storedRow(page, t, name)).toBeVisible();
      // Who uploaded it is not shown to external users.
      await expect(
        page.getByRole("columnheader", { name: t("createdBy") })
      ).toHaveCount(0);

      const download = page.waitForEvent("download");
      await storedRow(page, t, name).click();
      expect((await download).suggestedFilename()).toBe(name);

      // A click anywhere else on the row opens the details of an own file, as in the React version.
      const renamed = `pf-e2e-public-renamed-${uniqueSuffix()}.txt`;
      await page
        .getByRole("row")
        .filter({ has: storedRow(page, t, name) })
        // Checkbox, name (its link downloads), then the size: a plain cell.
        .getByRole("cell")
        .nth(2)
        .click();
      await page
        .getByRole("textbox", { name: t("attachment.fileName") })
        .fill(renamed);
      await page.getByRole("button", { name: t("save"), exact: true }).click();
      await expect(storedRow(page, t, renamed)).toBeVisible();
      await expect(storedRow(page, t, name)).toHaveCount(0);
      name = renamed;

      await page
        .getByRole("button", { name: `${t("delete")}: ${name}` })
        .click();
      await page
        .getByRole("alertdialog")
        .getByRole("button", { name: t("delete"), exact: true })
        .click();
      await expect(storedRow(page, t, name)).toHaveCount(0);

      await page.getByRole("button", { name: t("menu.logout") }).click();
      await expect(
        page.locator('[role="alert"][data-tone="success"]')
      ).toBeVisible();
      await expect(page.locator("#externalPassword")).toBeVisible();
      // The session is gone: a reload stays on the login.
      await page.reload();
      await expect(page.locator("#externalPassword")).toBeVisible();
    } finally {
      await deleteEntity(seedRequest, "datatransfer", area.id).catch(
        () => undefined
      );
    }
  });

  test("offers no upload in an area with download access only", async ({
    page,
    seedRequest,
  }) => {
    const area = await createArea(seedRequest, {
      download: true,
      upload: false,
    });
    try {
      await goto(page, `/public/datatransfer/${area.token}`);
      await waitForHydration(page);
      const t = await publicFormat(page);
      await login(page, PASSWORD);
      await expect(
        page.getByRole("heading", { name: area.areaName })
      ).toBeVisible();
      await expect(page.getByLabel(t("file.upload.choose"))).toHaveCount(0);
    } finally {
      await deleteEntity(seedRequest, "datatransfer", area.id).catch(
        () => undefined
      );
    }
  });
});
