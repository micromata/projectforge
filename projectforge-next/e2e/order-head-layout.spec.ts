import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";

/**
 * The head section of an order: the number and the date of the offer share one cell of the three
 * columns, so the section keeps reading in three columns — see [FieldGroupDeclaration].
 *
 * Nothing is saved; the form is opened and abandoned.
 */
test.describe("order head layout", () => {
  test("puts the number and the offer date side by side", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/order/new");
    // `._`: "fibu.auftrag.nummer" is a text of its own *and* the parent of `…nummer.short`, which the
    // generator can only export as a nested object plus a `_` leaf (see labelKeyFor).
    const nummer = page.getByRole("textbox", {
      name: format.t("fibu.auftrag.nummer._"),
    });
    const angebot = page.getByRole("textbox", {
      name: format.t("fibu.auftrag.angebot.datum"),
    });
    const status = page.getByRole("combobox", {
      name: format.t("status"),
    });
    await expect(nummer).toBeVisible();
    await expect(angebot).toBeVisible();

    const [numberBox, angebotBox, statusBox] = await Promise.all([
      nummer.boundingBox(),
      angebot.boundingBox(),
      status.boundingBox(),
    ]);
    if (!numberBox || !angebotBox || !statusBox)
      throw new Error("a field of the head section is not laid out");

    // Same row: the offer date sits right of the number, not underneath it.
    expect(angebotBox.x).toBeGreaterThan(numberBox.x + numberBox.width);
    expect(Math.abs(angebotBox.y - numberBox.y)).toBeLessThan(
      numberBox.height / 2
    );
    // And both stay inside the first of the three columns, i.e. left of the status.
    expect(angebotBox.x + angebotBox.width).toBeLessThan(statusBox.x);
    // Same row as the status, too — the group must not shift the rest of the grid.
    expect(Math.abs(statusBox.y - numberBox.y)).toBeLessThan(numberBox.height);

    // A new order starts in its title: the number is read-only and a date opens its calendar when it
    // takes the cursor, so neither is where a form begins (see useFocusFirstField).
    await expect(
      page.getByRole("textbox", { name: format.t("fibu.auftrag.title._") })
    ).toBeFocused();
  });
});
