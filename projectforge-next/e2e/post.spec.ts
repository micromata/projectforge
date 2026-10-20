import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat } from "./fixtures/format";
import { POSTAUSGANG_METADATA } from "../lib/metadata/postausgang.generated";
import { POSTEINGANG_METADATA } from "../lib/metadata/posteingang.generated";
import { POSTAUSGANG_PAGE } from "../components/features/post/postausgang.page";
import { POSTEINGANG_PAGE } from "../components/features/post/posteingang.page";
import { columnHeaderKeyOf, columnIdOf } from "../lib/page-def/define-page";
import type { ColumnDeclaration } from "../lib/page-def/types";
import type { EntityMetadata } from "../lib/metadata/types";

/**
 * The inbox and the outbox against the live backend (see POSTEINGANG_PAGE, POSTAUSGANG_PAGE and the two
 * EntityRests).
 *
 * The database may be a copy of production: nothing is written. The save test intercepts the save request and
 * checks what the form would have posted: the preset of the backend (type) and the texts typed.
 */
const BOOKS = [
  {
    entity: "incomingMail",
    titleKey: POSTEINGANG_PAGE.titleKey,
    columns: shownColumns(POSTEINGANG_PAGE.columns, POSTEINGANG_METADATA),
    // The mandatory texts besides the preset date and type.
    texts: { absender: "e2e sender", inhalt: "e2e content" },
    presetType: "E_MAIL",
  },
  {
    entity: "outgoingMail",
    titleKey: POSTAUSGANG_PAGE.titleKey,
    columns: shownColumns(POSTAUSGANG_PAGE.columns, POSTAUSGANG_METADATA),
    texts: { empfaenger: "e2e receiver", inhalt: "e2e content" },
    presetType: "BRIEF",
  },
] as const;

for (const book of BOOKS) {
  test.describe(book.entity, { tag: "@parallel" }, () => {
    test("shows the declared columns under the labels of the entity", async ({
      loggedInPage: page,
    }) => {
      const format = await userFormat(page);
      await goto(page, `/${book.entity}`);

      await expect(
        page.getByRole("heading", { name: format.t(book.titleKey) })
      ).toBeVisible();
      for (const column of book.columns) {
        await expect(
          page.getByRole("columnheader", {
            name: new RegExp(`^${escape(label(format, column.key))}`),
          }),
          `column ${column.id}`
        ).toHaveCount(1);
      }
    });

    test("posts a new entry with the preset type on save", async ({
      loggedInPage: page,
    }) => {
      const format = await userFormat(page);
      let posted: Record<string, unknown> | undefined;
      await page.route(`**/rs/${book.entity}/{insert,update}*`, (route) => {
        const body = route.request().postDataJSON() as {
          data?: Record<string, unknown>;
        };
        posted = body.data ?? (body as Record<string, unknown>);
        return route.abort();
      });

      await goto(page, `/${book.entity}/new`);
      for (const [field, text] of Object.entries(book.texts)) {
        await page.locator(`[data-field="${field}"] input`).fill(text);
      }
      await page.getByRole("button", { name: format.t("create") }).click();

      await expect
        .poll(() => posted, { message: "the save reaches the backend" })
        .toBeDefined();
      expect(posted?.type).toBe(book.presetType);
      expect(posted?.datum).toBeTruthy();
      for (const [field, text] of Object.entries(book.texts)) {
        expect(posted?.[field]).toBe(text);
      }
    });

    test("redirects the old React list", async ({ loggedInPage: page }) => {
      await page.goto(`/react/${book.entity}`);
      await expect(page).toHaveURL(new RegExp(`/next/${book.entity}$`));
    });
  });
}

/** The columns a list shows by default, with the key of their header label. */
function shownColumns<Row, M extends EntityMetadata>(
  columns: readonly ColumnDeclaration<Row, M>[],
  metadata: M
): { id: string; key: string }[] {
  return columns
    .filter((column) => !column.hiddenByDefault)
    .map((column) => ({
      id: columnIdOf(column),
      key: columnHeaderKeyOf(column, metadata),
    }));
}

function escape(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
