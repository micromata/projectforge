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

/**
 * The stored row of one file, addressed by its download link.
 *
 * Not by its name: while a file is uploading — and after a refused upload — a second row carries the
 * same name (see AttachmentUploadRow), and the backend's refusal message repeats it once more in a
 * toast. Only the stored rows offer a download.
 */
function storedRow(page: import("@playwright/test").Page, name: string) {
  return page.getByRole("link", { name: `Download: ${name}` });
}

/** Uploads through the section's file input, which the add button keeps `sr-only`. */
async function upload(page: import("@playwright/test").Page, name: string) {
  await page.getByLabel(/datei wählen/i).setInputFiles({ name, ...FILE });
  await expect(storedRow(page, name)).toBeVisible();
}

/** Removes an attachment again — also the cleanup, so it must not depend on the test's own state. */
async function remove(page: import("@playwright/test").Page, name: string) {
  await page.getByRole("button", { name: `Löschen: ${name}` }).click();
  await page
    .getByRole("button", { name: /^löschen$/i })
    .last()
    .click();
  await expect(storedRow(page, name)).toHaveCount(0);
}

test.describe("book attachments", () => {
  test("uploads, renames and deletes a file", async ({
    loggedInPage: page,
  }) => {
    await goto(page, `/book/${BOOK_ID}`);
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
      await expect(storedRow(page, renamed)).toBeVisible();
      await expect(page.getByText("e2e description")).toBeVisible();
      await expect(storedRow(page, name)).toHaveCount(0);

      await remove(page, renamed);
    } finally {
      // A failed assertion must not leave the file on a real book.
      for (const leftover of [name, renamed]) {
        if (await storedRow(page, leftover).count()) {
          await remove(page, leftover);
        }
      }
    }
  });

  test("shows the backend's message when the same file is uploaded twice", async ({
    loggedInPage: page,
  }) => {
    await goto(page, `/book/${BOOK_ID}`);
    const name = fileName("duplicate");

    try {
      await upload(page, name);
      // The refusal arrives as a regular HTTP 200 carrying a TOAST (see lib/rs/attachments.ts) —
      // the regression this guards is treating it as success, which would drop the file silently.
      await page.getByLabel(/datei wählen/i).setInputFiles({ name, ...FILE });
      // The refused file keeps a row of its own carrying the reason, so it stays clear *which* file
      // was turned away. The same text also appears in a toast, hence the row-scoped locator.
      const refused = page
        .getByRole("listitem")
        .filter({ hasText: /existiert bereits/i });
      await expect(refused).toHaveCount(1);
      // Dismissing it is the user's move, and it leaves the one stored file untouched.
      await page.getByRole("button", { name: `Abbrechen: ${name}` }).click();
      await expect(refused).toHaveCount(0);
      await expect(storedRow(page, name)).toBeVisible();
    } finally {
      await remove(page, name);
    }
  });

  test("shows the metadata the backend recorded for a file", async ({
    loggedInPage: page,
  }) => {
    await goto(page, `/book/${BOOK_ID}`);
    const name = fileName("metadata");

    try {
      await upload(page, name);
      await page.getByRole("button", { name: `Bearbeiten: ${name}` }).click();

      const dialog = page.getByRole("dialog");
      // Everything below is the backend's own wording and formatting — the point of the assertions
      // is that the fields arrive at all (the upload answer carries them, see AttachmentMetadata).
      await expect(dialog.getByText("Dateigröße")).toBeVisible();
      await expect(dialog.getByText("ohne Verschlüsselung")).toBeVisible();
      await expect(dialog.getByText("angelegt", { exact: true })).toBeVisible();
      await expect(dialog.getByText(/SHA256:/)).toBeVisible();
      await expect(
        dialog.getByRole("button", { name: /kopieren: prüfsumme/i })
      ).toBeVisible();

      await page.getByRole("button", { name: /^abbrechen$/i }).click();
    } finally {
      await remove(page, name);
    }
  });

  test("downloads and deletes a whole selection", async ({
    loggedInPage: page,
  }) => {
    await goto(page, `/book/${BOOK_ID}`);
    const names = [fileName("multi-a"), fileName("multi-b")];

    try {
      for (const name of names) await upload(page, name);

      // The rows are picked one by one, not through select-all: the book is a real one and may
      // already carry attachments this test must not touch.
      for (const name of names) {
        await page
          .getByRole("checkbox", { name: `Auswählen: ${name}` })
          .click();
      }

      // One ZIP for the selection (multiDownload), so what arrives is a .zip — the files' own names
      // are inside it, out of the test's reach.
      const download = page.waitForEvent("download");
      await page
        .getByRole("link", { name: /ausgewählte herunterladen/i })
        .click();
      expect((await download).suggestedFilename()).toMatch(/\.zip$/);

      await page.getByRole("button", { name: /ausgewählte löschen/i }).click();
      // The plural question, not the single row's: multiDelete is just as final, but it says how
      // many files it takes.
      await expect(
        page.getByText(/alle ausgewählten dateien unwiderruflich/i)
      ).toBeVisible();
      await page
        .getByRole("button", { name: /^löschen$/i })
        .last()
        .click();

      // multiDelete answers with the one list that remains, so both rows go in a single update.
      for (const name of names) {
        await expect(storedRow(page, name)).toHaveCount(0);
      }
    } finally {
      for (const leftover of names) {
        if (await storedRow(page, leftover).count()) {
          await remove(page, leftover);
        }
      }
    }
  });

  test("downloads all files and opens the details on a row click", async ({
    loggedInPage: page,
  }) => {
    await goto(page, `/book/${BOOK_ID}`);
    const name = fileName("row-click");

    try {
      await upload(page, name);

      // "Alle herunterladen" needs no selection — it asks for the ZIP of every file (see
      // AttachmentSelectionBar).
      const all = page.waitForEvent("download");
      await page.getByRole("link", { name: /alle herunterladen/i }).click();
      expect((await all).suggestedFilename()).toMatch(/\.zip$/);

      // The name is the file itself, not the dialog: the shortest way to a download.
      const single = page.waitForEvent("download");
      await storedRow(page, name).click();
      expect((await single).suggestedFilename()).toBe(name);
      await expect(page.getByRole("dialog")).toHaveCount(0);

      // Anywhere else in the row opens the details — the row overlay lies under the name link and
      // the two buttons, so a click next to the metadata reaches it.
      await page
        .getByRole("listitem")
        .filter({ hasText: name })
        .click({ position: { x: 300, y: 26 } });
      await expect(
        page.getByRole("dialog").getByRole("textbox", { name: /dateiname/i })
      ).toHaveValue(name);
      await page.getByRole("button", { name: /^abbrechen$/i }).click();
    } finally {
      await remove(page, name);
    }
  });

  test("says attachments need a saved book when adding one", async ({
    loggedInPage: page,
  }) => {
    await goto(page, "/book/new");
    // No id to hang a file off yet, so the section explains itself instead of offering an upload.
    await expect(
      page.getByText(/erst hochgeladen werden, nachdem/i)
    ).toBeVisible();
    await expect(page.getByLabel(/datei wählen/i)).toHaveCount(0);
  });
});
