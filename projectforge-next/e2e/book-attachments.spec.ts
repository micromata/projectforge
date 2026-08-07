import { test, expect, goto } from "./fixtures/auth";

/**
 * The attachments section of the book edit page, against the live JCR.
 *
 * Unlike the other book specs this one writes: there is no way to see an upload, a rename or the
 * backend's duplicate check without performing them. Everything it creates it deletes again through
 * the UI, and the file names carry the `pf-e2e-` prefix so a leftover is recognizable.
 */
const BOOK_ID = 316163;

/** Distinct per run: the backend refuses a name that is already attached, which is the point of the last test. */
function fileName(suffix: string): string {
  return `pf-e2e-${suffix}.txt`;
}

const FILE = {
  mimeType: "text/plain",
  buffer: Buffer.from("ProjectForge e2e attachment\n"),
};

/** Uploads through the section's file input, which the drop area keeps `sr-only`. */
async function upload(page: import("@playwright/test").Page, name: string) {
  await page.getByLabel(/datei wählen/i).setInputFiles({ name, ...FILE });
  await expect(page.getByText(name, { exact: true })).toBeVisible();
}

/** Removes an attachment again — also the cleanup, so it must not depend on the test's own state. */
async function remove(page: import("@playwright/test").Page, name: string) {
  await page.getByRole("button", { name: `Löschen: ${name}` }).click();
  await page
    .getByRole("button", { name: /^löschen$/i })
    .last()
    .click();
  await expect(page.getByText(name, { exact: true })).toHaveCount(0);
}

test.describe("book attachments", () => {
  test("uploads, renames and deletes a file", async ({
    loggedInPage: page,
  }) => {
    await goto(page, `/books/${BOOK_ID}`);
    const name = fileName("upload");
    const renamed = fileName("renamed");

    try {
      await upload(page, name);

      // The row's actions all repeat per file, so each carries the name — that is what makes them
      // addressable here and distinguishable for a screen reader.
      await page.getByRole("button", { name: `Bearbeiten: ${name}` }).click();
      await page.getByRole("textbox", { name: /dateiname/i }).fill(renamed);
      await page
        .getByRole("textbox", { name: /beschreibung/i })
        .fill("e2e description");
      await page.getByRole("button", { name: /^speichern$/i }).click();

      // `modify` answers with the entity's whole new list, so the rename shows without a re-read.
      await expect(page.getByText(renamed, { exact: true })).toBeVisible();
      await expect(page.getByText("e2e description")).toBeVisible();
      await expect(page.getByText(name, { exact: true })).toHaveCount(0);

      await remove(page, renamed);
    } finally {
      // A failed assertion must not leave the file on a real book.
      for (const leftover of [name, renamed]) {
        if (await page.getByText(leftover, { exact: true }).count()) {
          await remove(page, leftover);
        }
      }
    }
  });

  test("shows the backend's message when the same file is uploaded twice", async ({
    loggedInPage: page,
  }) => {
    await goto(page, `/books/${BOOK_ID}`);
    const name = fileName("duplicate");

    try {
      await upload(page, name);
      // The refusal arrives as a regular HTTP 200 carrying a TOAST (see lib/rs/attachments.ts) —
      // the regression this guards is treating it as success, which would drop the file silently.
      await page.getByLabel(/datei wählen/i).setInputFiles({ name, ...FILE });
      await expect(page.getByText(/existiert bereits/i)).toBeVisible();
    } finally {
      await remove(page, name);
    }
  });

  test("says attachments need a saved book when adding one", async ({
    loggedInPage: page,
  }) => {
    await goto(page, "/books/new");
    // No id to hang a file off yet, so the section explains itself instead of offering an upload.
    await expect(
      page.getByText(/erst hochgeladen werden, nachdem/i)
    ).toBeVisible();
    await expect(page.getByLabel(/datei wählen/i)).toHaveCount(0);
  });
});
