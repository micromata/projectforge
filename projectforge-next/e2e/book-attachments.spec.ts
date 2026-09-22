import type { Page } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { userFormat, type UserFormat } from "./fixtures/format";
import { purgeTestAttachments } from "./fixtures/attachments";
import { createBook, type SeededBook } from "./fixtures/seed";

/**
 * The attachments section of the book edit page, against the live JCR.
 *
 * Unlike the other book specs this one writes: there is no way to see an upload, a rename or the
 * backend's duplicate check without performing them. Everything it creates it deletes again through
 * the UI, and the file names carry the `pf-e2e-` prefix so a leftover is recognizable.
 *
 * A book of its own (`createBook`): the files go onto a real JCR node, and doing that to a row of the
 * production copy would mean uploading into a customer's document set.
 */

/** Distinct per run: the backend refuses a name that is already attached, which is the point of the last test. */
function fileName(suffix: string): string {
  return `pf-e2e-${suffix}.txt`;
}

const FILE = {
  mimeType: "text/plain",
  buffer: Buffer.from("ProjectForge e2e attachment\n"),
};

/**
 * The stored row of one file, addressed by its download link.
 *
 * Not by its name: while a file is uploading — and after a refused upload — a second row carries the
 * same name (see AttachmentUploadRow), and the backend's refusal message repeats it once more in a
 * toast. Only the stored rows offer a download.
 */
function storedRow(page: Page, t: UserFormat["t"], name: string) {
  return page.getByRole("link", { name: `${t("download._")}: ${name}` });
}

/** Uploads through the section's file input, which the add button keeps `sr-only`. */
async function upload(page: Page, t: UserFormat["t"], name: string) {
  await page
    .getByLabel(t("file.upload.choose"))
    .setInputFiles({ name, ...FILE });
  await expect(storedRow(page, t, name)).toBeVisible();
}

/** Removes an attachment again — also the cleanup, so it must not depend on the test's own state. */
async function remove(page: Page, t: UserFormat["t"], name: string) {
  await page.getByRole("button", { name: `${t("delete")}: ${name}` }).click();
  // The confirmation's button carries the bare label, unlike the row's — and is looked up inside the
  // dialog rather than as the last one on the page: before the dialog has mounted, "the last delete
  // button" is still the row's, and clicking that only reopens the question.
  await page
    .getByRole("alertdialog")
    .getByRole("button", { name: t("delete"), exact: true })
    .click();
  await expect(storedRow(page, t, name)).toHaveCount(0);
}

test.describe("book attachments", () => {
  let book: SeededBook;

  // One book for the file, not one per case: every case cleans its own files up again, and each
  // insert stays in the database (see fixtures/seed.ts).
  test.beforeAll(async ({ seedRequest }) => {
    book = await createBook(seedRequest);
  });

  // A failed case may have left a file behind — through the API rather than the UI, so a leftover
  // cannot make the *next* case fail on a count or on a duplicate name. Only `pf-e2e-` files go.
  test.beforeEach(async ({ loggedInPage: page }) => {
    await purgeTestAttachments(page, "book", book.id);
  });

  test("uploads, renames and deletes a file", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, `/book/${book.id}`);
    const name = fileName("upload");
    const renamed = fileName("renamed");

    try {
      await upload(page, t, name);

      // The row's actions all repeat per file, so each carries the name — that is what makes them
      // addressable here and distinguishable for a screen reader.
      await page.getByRole("button", { name: `${t("edit")}: ${name}` }).click();
      await page
        .getByRole("textbox", { name: t("attachment.fileName") })
        .fill(renamed);
      await page
        .getByRole("textbox", { name: t("description") })
        .fill("e2e description");
      await page.getByRole("button", { name: t("save"), exact: true }).click();

      // `modify` answers with the entity's whole new list, so the rename shows without a re-read.
      await expect(storedRow(page, t, renamed)).toBeVisible();
      await expect(page.getByText("e2e description")).toBeVisible();
      await expect(storedRow(page, t, name)).toHaveCount(0);

      await remove(page, t, renamed);
    } finally {
      // A failed assertion must not leave the file behind, or the duplicate case below sees it.
      for (const leftover of [name, renamed]) {
        if (await storedRow(page, t, leftover).count()) {
          await remove(page, t, leftover);
        }
      }
    }
  });

  test("shows the backend's message when the same file is uploaded twice", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, `/book/${book.id}`);
    const name = fileName("duplicate");

    try {
      await upload(page, t, name);
      // The refusal arrives as a regular HTTP 200 carrying a TOAST (see lib/rs/attachments.ts) —
      // the regression this guards is treating it as success, which would drop the file silently.
      await page
        .getByLabel(t("file.upload.choose"))
        .setInputFiles({ name, ...FILE });
      // The refused file keeps a row of its own carrying the reason, so it stays clear *which* file
      // was turned away. Scoped to `main`, because the same text also arrives as a toast and sonner
      // renders one as a `<li>` too (in its own `<ol>` next to the page, see app/layout.tsx) — an
      // unscoped listitem locator counts the row and the toast, and it does so for a while: the
      // toast outlives the row's own lifetime.
      const refused = page
        .locator("main")
        .getByRole("listitem")
        .filter({
          hasText: t("file.upload.error.fileAlreadyExists", { arg0: name }),
        });
      await expect(refused).toHaveCount(1);
      // Dismissing it is the user's move, and it leaves the one stored file untouched.
      await page
        .getByRole("button", { name: `${t("cancel")}: ${name}` })
        .click();
      await expect(refused).toHaveCount(0);
      await expect(storedRow(page, t, name)).toBeVisible();
    } finally {
      await remove(page, t, name);
    }
  });

  test("shows the metadata the backend recorded for a file", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, `/book/${book.id}`);
    const name = fileName("metadata");

    try {
      await upload(page, t, name);
      await page.getByRole("button", { name: `${t("edit")}: ${name}` }).click();

      const dialog = page.getByRole("dialog");
      // Everything below is the backend's own wording and formatting — the point of the assertions
      // is that the fields arrive at all (the upload answer carries them, see AttachmentMetadata).
      await expect(dialog.getByText(t("attachment.fileSize"))).toBeVisible();
      // A file that was never encrypted has no zipMode, which the section reads as the STANDARD one.
      await expect(
        dialog.getByText(t("attachment.zip.standard"))
      ).toBeVisible();
      await expect(
        dialog.getByText(t("created"), { exact: true })
      ).toBeVisible();
      await expect(dialog.getByText(/SHA256:/)).toBeVisible();
      await expect(
        dialog.getByRole("button", {
          name: `${t("copy")}: ${t("attachment.checksum")}`,
        })
      ).toBeVisible();

      await page
        .getByRole("button", { name: t("cancel"), exact: true })
        .click();
    } finally {
      await remove(page, t, name);
    }
  });

  test("downloads and deletes a whole selection", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, `/book/${book.id}`);
    const names = [fileName("multi-a"), fileName("multi-b")];

    try {
      for (const name of names) await upload(page, t, name);

      // The rows are picked one by one rather than through select-all: what is asserted below is that
      // a *selection* is downloaded and deleted, which select-all would not distinguish from "all".
      for (const name of names) {
        await page
          .getByRole("checkbox", { name: `${t("select._")}: ${name}` })
          .click();
      }

      // One ZIP for the selection (multiDownload), so what arrives is a .zip — the files' own names
      // are inside it, out of the test's reach.
      const download = page.waitForEvent("download");
      await page
        .getByRole("link", { name: t("file.upload.downloadSelected") })
        .click();
      expect((await download).suggestedFilename()).toMatch(/\.zip$/);

      await page
        .getByRole("button", { name: t("file.upload.deleteSelected._") })
        .click();
      // The plural question, not the single row's: multiDelete is just as final, but it says how
      // many files it takes.
      await expect(
        page.getByText(t("file.upload.deleteSelected.confirm"))
      ).toBeVisible();
      await page
        .getByRole("button", { name: t("delete"), exact: true })
        .last()
        .click();

      // multiDelete answers with the one list that remains, so both rows go in a single update.
      for (const name of names) {
        await expect(storedRow(page, t, name)).toHaveCount(0);
      }
    } finally {
      for (const leftover of names) {
        if (await storedRow(page, t, leftover).count()) {
          await remove(page, t, leftover);
        }
      }
    }
  });

  test("downloads all files and opens the details on a row click", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, `/book/${book.id}`);
    const name = fileName("row-click");

    try {
      await upload(page, t, name);

      // "Download all" needs no selection — it asks for the ZIP of every file (see
      // AttachmentSelectionBar).
      const all = page.waitForEvent("download");
      await page
        .getByRole("link", { name: t("attachment.downloadAll") })
        .click();
      expect((await all).suggestedFilename()).toMatch(/\.zip$/);

      // The name is the file itself, not the dialog: the shortest way to a download.
      const single = page.waitForEvent("download");
      await storedRow(page, t, name).click();
      expect((await single).suggestedFilename()).toBe(name);
      await expect(page.getByRole("dialog")).toHaveCount(0);

      // Anywhere else in the row opens the details — the row overlay lies under the name link and
      // the two buttons, so a click next to the metadata reaches it.
      await page
        .getByRole("listitem")
        .filter({ hasText: name })
        .click({ position: { x: 300, y: 26 } });
      await expect(
        page
          .getByRole("dialog")
          .getByRole("textbox", { name: t("attachment.fileName") })
      ).toHaveValue(name);
      await page
        .getByRole("button", { name: t("cancel"), exact: true })
        .click();
    } finally {
      await remove(page, t, name);
    }
  });

  test("says attachments need a saved book when adding one", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, "/book/new");
    // No id to hang a file off yet, so the section explains itself instead of offering an upload.
    await expect(
      page.getByText(t("attachment.onlyAvailableAfterSave"))
    ).toBeVisible();
    await expect(page.getByLabel(t("file.upload.choose"))).toHaveCount(0);
  });
});
