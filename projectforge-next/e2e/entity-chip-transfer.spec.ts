import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";

/**
 * Chips of a multi picker dragged into another picker (see useChipTransfer): taken by a picker of the
 * same kind, refused by one of another. On the access fields of a new team calendar, which is never saved.
 */
test.describe("entity chip transfer", { tag: "@parallel" }, () => {
  test("drags a user into another user field, not into a group field", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, "/teamCal/new");
    const users = t("plugins.teamcal.access.users");
    const groups = t("plugins.teamcal.access.groups");
    const box = (access: string, kind: string) =>
      page.getByRole("listbox", {
        name: `${t(access)} (${kind})`,
        exact: true,
      });
    const fullUsers = box("plugins.teamcal.fullAccess", users);
    const readonlyUsers = box("plugins.teamcal.readonlyAccess", users);
    const readonlyGroups = box("plugins.teamcal.readonlyAccess", groups);

    // The full access field offers the logged-in user as a one-click pick, the first such button.
    await page
      .getByRole("button", { name: new RegExp(`^${t("select._")}: `) })
      .first()
      .click();
    const me = fullUsers.getByRole("option");
    await expect(me).toHaveCount(1);
    const name = (await me.innerText()).trim();

    await me.dragTo(readonlyGroups);
    await expect(readonlyGroups.getByRole("option")).toHaveCount(0);

    await me.dragTo(readonlyUsers);
    await expect(readonlyUsers.getByRole("option")).toHaveText([name]);
    // Copied, not moved — and a second drop adds no duplicate.
    await expect(fullUsers.getByRole("option")).toHaveCount(1);
    await me.dragTo(readonlyUsers);
    await expect(readonlyUsers.getByRole("option")).toHaveCount(1);

    // A click selects the chip, Delete removes what is selected.
    await readonlyUsers.getByRole("option").click();
    await expect(readonlyUsers.getByRole("option")).toHaveAttribute(
      "aria-selected",
      "true"
    );
    await page.keyboard.press("Delete");
    await expect(readonlyUsers.getByRole("option")).toHaveCount(0);
  });

  test("copies the chips with the keyboard and pastes them into a field of the same kind", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, "/teamCal/new");
    const users = t("plugins.teamcal.access.users");
    const box = (access: string) =>
      page.getByRole("listbox", {
        name: `${t(access)} (${users})`,
        exact: true,
      });
    const fullUsers = box("plugins.teamcal.fullAccess");
    const minimalUsers = box("plugins.teamcal.minimalAccess._");

    await page
      .getByRole("button", { name: new RegExp(`^${t("select._")}: `) })
      .first()
      .click();
    await expect(fullUsers.getByRole("option")).toHaveCount(1);

    // Nothing selected: Ctrl/Cmd+C copies all chips of the field.
    await fullUsers.focus();
    await page.keyboard.press("ControlOrMeta+C");
    await minimalUsers.focus();
    await page.keyboard.press("ControlOrMeta+V");
    await expect(minimalUsers.getByRole("option")).toHaveCount(1);
  });
});
