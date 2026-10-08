import type { APIRequestContext, Page } from "@playwright/test";
import { test, expect, goto, login } from "./fixtures/auth";
import { hasRole } from "./fixtures/credentials";
import { userFormat, type UserFormat } from "./fixtures/format";
import { purgeTestAttachments } from "./fixtures/attachments";
import { secretPeek } from "../lib/secret-peek";
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

function tab(page: Page, name: string) {
  return page.getByRole("tab", { name, exact: true });
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
      // The files are the default tab; the admin of the area also gets its form.
      await expect(
        tab(page, t("plugins.datatransfer.tab.files"))
      ).toHaveAttribute("aria-selected", "true");
      await expect(tab(page, t("edit"))).toBeVisible();

      await page
        .getByLabel(t("file.upload.choose"))
        .setInputFiles({ name, ...FILE });
      await expect(storedRow(page, t, name)).toBeVisible();

      // Observing sits in the info tab and is stored at once, so it survives a reload.
      await tab(page, t("plugins.datatransfer.tab.info")).click();
      await expect(page).toHaveURL(/\?tab=info$/);
      const observe = page.getByLabel(
        t("plugins.datatransfer.userWantsToObserve._")
      );
      // The box is checked at once, before the server has answered: a reload right away would abort
      // the request still in flight.
      const stored = page.waitForResponse(
        (res) =>
          res.url().includes(`/rs/datatransferfiles/observe/${area.id}`) &&
          res.ok()
      );
      await observe.click();
      await expect(observe).toBeChecked();
      await stored;
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

      await tab(page, t("plugins.datatransfer.tab.files")).click();
      await removeFile(page, t, name);
    } finally {
      await purgeTestAttachments(page, "datatransfer", area.id);
      await markAsDeleted(seedRequest, "datatransfer", area.id).catch(
        () => undefined
      );
    }
  });

  test("an access user sees the files but no admin form", async ({
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
      // Asked for by url, the form is still not shown: the files are.
      await goto(page, `/datatransfer/${area.id}?tab=edit`);
      await expect(
        page.getByRole("heading", { name: area.areaName })
      ).toBeVisible();
      await expect(
        tab(page, t("plugins.datatransfer.tab.files"))
      ).toHaveAttribute("aria-selected", "true");
      await expect(tab(page, t("edit"))).toHaveCount(0);
      await expect(page.getByRole("button", { name: t("save") })).toHaveCount(
        0
      );
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
    await expect(tab(page, t("plugins.datatransfer.tab.files"))).toBeVisible();
    await expect(tab(page, t("edit"))).toHaveCount(0);
  });

  test("edits the area in its tab and keeps unsaved input across tabs", async ({
    loggedInPage: page,
    seedRequest,
  }) => {
    const area = await createArea(seedRequest, await userIdOf(page.request));
    const { t } = await userFormat(page);
    const description = `${MARKER} description ${uniqueSuffix()}`;
    try {
      // The old edit url forwards to the tab.
      await goto(page, `/datatransfer/${area.id}/edit`);
      await expect(page).toHaveURL(
        new RegExp(`/datatransfer/${area.id}/?\\?tab=edit$`)
      );
      const field = page.getByRole("textbox", { name: t("description") });
      await field.fill(description);

      // A look at the files does not throw the input away.
      await tab(page, t("plugins.datatransfer.tab.files")).click();
      await expect(page.getByLabel(t("file.upload.choose"))).toBeAttached();
      await tab(page, t("edit")).click();
      await expect(field).toHaveValue(description);

      await page.getByRole("button", { name: t("save"), exact: true }).click();
      // Saved, the page returns to the files, and the info shows what was stored.
      await expect(page).toHaveURL(new RegExp(`/datatransfer/${area.id}/?$`));
      await tab(page, t("plugins.datatransfer.tab.info")).click();
      await expect(page.getByText(description)).toBeVisible();
    } finally {
      await markAsDeleted(seedRequest, "datatransfer", area.id).catch(
        () => undefined
      );
    }
  });

  test("shows a link as text to copy, not as a box to type into", async ({
    loggedInPage: page,
    seedRequest,
  }) => {
    const area = await createArea(seedRequest, await userIdOf(page.request));
    const { t } = await userFormat(page);
    const linkLabel = t("plugins.datatransfer.internal.link");
    await page
      .context()
      .grantPermissions(["clipboard-read", "clipboard-write"]);
    try {
      await goto(page, `/datatransfer/${area.id}?tab=info`);
      const link = page.getByRole("group", { name: linkLabel, exact: true });
      await expect(link).toContainText(`/datatransfer/${area.id}`);
      await expect(link.getByRole("textbox")).toHaveCount(0);
      await link
        .getByRole("button", { name: `${t("copy")}: ${linkLabel}` })
        .click();
      expect(await page.evaluate(() => navigator.clipboard.readText())).toBe(
        (await link.textContent())?.trim()
      );
    } finally {
      await markAsDeleted(seedRequest, "datatransfer", area.id).catch(
        () => undefined
      );
    }
  });

  test("hides the external password until revealed and copies it again and again", async ({
    loggedInPage: page,
    seedRequest,
  }) => {
    const area = await createArea(seedRequest, await userIdOf(page.request));
    const { t } = await userFormat(page);
    const passwordLabel = t("plugins.datatransfer.external.password._");
    await page
      .context()
      .grantPermissions(["clipboard-read", "clipboard-write"]);
    try {
      await goto(page, `/datatransfer/${area.id}?tab=edit`);
      await page
        .getByRole("checkbox", {
          name: t("plugins.datatransfer.external.download.enabled._"),
        })
        .check();
      // Switching external access on fills in a password (useExternalSecrets).
      const password = page.getByLabel(passwordLabel, { exact: true });
      await expect(password).not.toHaveValue("");
      // Hidden first, with its beginning showing through; the eye shows it and hides it again.
      await expect(password).toHaveAttribute("type", "password");
      const value = await password.inputValue();
      await expect(page.locator('[data-slot="secret-peek"]')).toHaveText(
        secretPeek(value)
      );
      // Typed into, the whole box is shown again (as dots), not only its beginning.
      await password.focus();
      await expect(page.locator('[data-slot="secret-peek"]')).toHaveCount(0);
      await password.blur();
      await expect(page.locator('[data-slot="secret-peek"]')).toHaveCount(1);
      await page
        .getByRole("button", { name: `${t("secret.show")}: ${passwordLabel}` })
        .click();
      await expect(password).toHaveAttribute("type", "text");
      await page
        .getByRole("button", { name: `${t("secret.hide")}: ${passwordLabel}` })
        .click();
      await expect(password).toHaveAttribute("type", "password");

      // Copied, the button says so — and becomes a copy button again, so it can be used once more.
      await page
        .getByRole("button", { name: `${t("copy")}: ${passwordLabel}` })
        .click();
      await expect(
        page.getByRole("button", { name: `${t("copied")}: ${passwordLabel}` })
      ).toBeVisible();
      expect(await page.evaluate(() => navigator.clipboard.readText())).toBe(
        value
      );
      await expect(
        page.getByRole("button", { name: `${t("copy")}: ${passwordLabel}` })
      ).toBeVisible({ timeout: 5000 });
      // Not saved: the area is removed below either way.
    } finally {
      await markAsDeleted(seedRequest, "datatransfer", area.id).catch(
        () => undefined
      );
    }
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
