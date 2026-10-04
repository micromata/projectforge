import type { APIRequestContext, Page } from "@playwright/test";
import { test, expect, goto, login } from "./fixtures/auth";
import { hasRole } from "./fixtures/credentials";
import { userFormat, type UserFormat } from "./fixtures/format";
import { purgeTestAttachments } from "./fixtures/attachments";
import { insert, markAsDeleted, MARKER, uniqueSuffix } from "./fixtures/seed";

/**
 * The internal data transfer pages (plugin datatransfer): the file view of an area, its activities, the
 * own personal box and the admin form's entry point.
 *
 * Every test works on an area of its own, created through the API and marked as deleted again — never on
 * a list (no stored list state, hence `@parallel`). Uploaded files carry the `pf-e2e-` prefix and are
 * deleted through the UI, or by `purgeTestAttachments` if a test died before.
 */

const FILE = {
  mimeType: "text/plain",
  buffer: Buffer.from("ProjectForge e2e data transfer\n"),
};

/** The id of the user `request` is logged in as. */
async function userIdOf(request: APIRequestContext): Promise<number> {
  const res = await request.get("/rs/userStatus", {
    headers: { "X-PF-Frontend": "next" },
  });
  const { userData } = (await res.json()) as { userData: { userId: number } };
  return userData.userId;
}

/** An area administered by `adminId`, readable by `accessUserIds`; the defaults of a new area otherwise. */
async function createArea(
  request: APIRequestContext,
  adminId: number,
  accessUserIds: number[] = []
): Promise<{ id: number; areaName: string }> {
  const areaName = `${MARKER} datatransfer ${uniqueSuffix()}`;
  const id = await insert(request, "datatransfer", {
    areaName,
    admins: [{ id: adminId }],
    accessUsers: accessUserIds.map((userId) => ({ id: userId })),
    expiryDays: 7,
    maxUploadSizeKB: 100 * 1024,
  });
  return { id, areaName };
}

/** The stored row of a file, by its download link (see book-attachments.spec.ts). */
function storedRow(page: Page, t: UserFormat["t"], name: string) {
  return page.getByRole("link", {
    name: `${t("download._")}: ${name}`,
    exact: true,
  });
}

async function removeFile(page: Page, t: UserFormat["t"], name: string) {
  await page.getByRole("button", { name: `${t("delete")}: ${name}` }).click();
  await page
    .getByRole("alertdialog")
    .getByRole("button", { name: t("delete"), exact: true })
    .click();
  await expect(storedRow(page, t, name)).toHaveCount(0);
}

test.describe("data transfer", { tag: "@parallel" }, () => {
  test("uploads and deletes a file, observes the area and shows its activities", async ({
    loggedInPage: page,
    seedRequest,
  }) => {
    const area = await createArea(seedRequest, await userIdOf(page.request));
    const { t } = await userFormat(page);
    const name = `pf-e2e-datatransfer-${uniqueSuffix()}.txt`;
    try {
      await goto(page, `/datatransfer/${area.id}`);
      await expect(
        page.getByRole("heading", { name: area.areaName })
      ).toBeVisible();
      // The admin of the area may edit it.
      await expect(
        page.getByRole("link", { name: t("plugins.datatransfer.title.edit") })
      ).toBeVisible();

      await page
        .getByLabel(t("file.upload.choose"))
        .setInputFiles({ name, ...FILE });
      await expect(storedRow(page, t, name)).toBeVisible();

      // Observing is stored at once, so it survives a reload.
      const observe = page.getByLabel(
        t("plugins.datatransfer.userWantsToObserve._")
      );
      await observe.click();
      await expect(observe).toBeChecked();
      await page.reload();
      await expect(observe).toBeChecked();
      await observe.click();
      await expect(observe).not.toBeChecked();

      await page
        .getByRole("button", { name: t("plugins.datatransfer.audit.display") })
        .click();
      const dialog = page.getByRole("dialog");
      await expect(
        dialog.getByText(
          `${t("plugins.datatransfer.audit._")}: ${area.areaName}`
        )
      ).toBeVisible();
      await expect(dialog.getByText(name).first()).toBeVisible();
      await page.keyboard.press("Escape");

      await removeFile(page, t, name);
    } finally {
      await purgeTestAttachments(page, "datatransfer", area.id);
      await markAsDeleted(seedRequest, "datatransfer", area.id).catch(
        () => undefined
      );
    }
  });

  test("an access user sees the files but no edit button", async ({
    page,
    seedRequest,
  }) => {
    test.skip(!hasRole("normalo-user"), "no normalo account on this instance");
    await login(page, "normalo-user");
    const area = await createArea(seedRequest, await userIdOf(seedRequest), [
      await userIdOf(page.request),
    ]);
    try {
      const { t } = await userFormat(page);
      await goto(page, `/datatransfer/${area.id}`);
      await expect(
        page.getByRole("heading", { name: area.areaName })
      ).toBeVisible();
      await expect(
        page.getByRole("button", {
          name: t("plugins.datatransfer.audit.display"),
        })
      ).toBeVisible();
      await expect(
        page.getByRole("link", { name: t("plugins.datatransfer.title.edit") })
      ).toHaveCount(0);
    } finally {
      await markAsDeleted(seedRequest, "datatransfer", area.id).catch(
        () => undefined
      );
    }
  });

  test("resolves the own personal box to its real id", async ({
    loggedInPage: page,
  }) => {
    await goto(page, "/datatransfer/-1");
    await expect(page).toHaveURL(/\/datatransfer\/[1-9]\d*\/?$/);
    // A personal box is not editable, not even by its owner.
    const { t } = await userFormat(page);
    await expect(
      page.getByRole("link", { name: t("plugins.datatransfer.title.edit") })
    ).toHaveCount(0);
  });

  test("redirects the link of an old notification mail", async ({
    loggedInPage: page,
    seedRequest,
  }) => {
    // OrphanedLinkFilter lives in Spring; the Next dev server does not serve /react at all.
    const port = new URL(test.info().project.use.baseURL ?? "").port;
    test.skip(
      process.env.E2E_DEV_SERVER === "1" || port === "3000",
      "needs Spring"
    );
    const area = await createArea(seedRequest, await userIdOf(page.request));
    try {
      await page.goto(`/react/datatransferfiles/dynamic/${area.id}`);
      await expect(page).toHaveURL(
        new RegExp(`/next/datatransfer/${area.id}/?$`)
      );
    } finally {
      await markAsDeleted(seedRequest, "datatransfer", area.id).catch(
        () => undefined
      );
    }
  });
});
