import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";
import { writeHeaders } from "./fixtures/seed";

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

  test("cuts the chips, pastes them elsewhere and undoes both, each in its field", async ({
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

    await fullUsers.focus();
    await page.keyboard.press("ControlOrMeta+X");
    await expect(fullUsers.getByRole("option")).toHaveCount(0);
    await minimalUsers.focus();
    await page.keyboard.press("ControlOrMeta+V");
    await expect(minimalUsers.getByRole("option")).toHaveCount(1);

    // Undo in the field pasted into takes the paste back, and only that.
    await page.keyboard.press("ControlOrMeta+Z");
    await expect(minimalUsers.getByRole("option")).toHaveCount(0);
    await expect(fullUsers.getByRole("option")).toHaveCount(0);
    // Undo in the field cut from brings the chip back, redo cuts it again.
    await fullUsers.focus();
    await page.keyboard.press("ControlOrMeta+Z");
    await expect(fullUsers.getByRole("option")).toHaveCount(1);
    await page.keyboard.press("ControlOrMeta+Shift+Z");
    await expect(fullUsers.getByRole("option")).toHaveCount(0);
  });

  test("sweeps over chips in both directions and copies just those", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    // A group of at least three members; a database without one has nothing to sweep over.
    const response = await page.request.post("/rs/group/list", {
      headers: await writeHeaders(page.request),
      data: { searchString: "" },
    });
    const { resultSet = [] } = (await response.json()) as {
      resultSet?: { id?: number; assignedUsers?: unknown[] }[];
    };
    const group = resultSet.find(
      (row) => (row.assignedUsers?.length ?? 0) >= 3
    );
    test.skip(!group?.id, "no group with three members");

    await goto(page, `/group/${group!.id}`);
    const members = page.getByRole("listbox", {
      name: t("group.assignedUsers"),
      exact: true,
    });
    const chips = members.getByRole("option");
    await expect(chips.nth(2)).toBeVisible();
    const boxRect = (await members.boundingBox())!;
    const second = (await chips.nth(1).boundingBox())!;
    const first = (await chips.nth(0).boundingBox())!;

    // Backwards, from behind the last chip — on the trigger filling the rest of the line — to the
    // middle of the second last: those two, and the search stays closed.
    const count = await chips.count();
    const lastRect = (await chips.nth(count - 1).boundingBox())!;
    const secondLast = (await chips.nth(count - 2).boundingBox())!;
    await page.mouse.move(
      lastRect.x + lastRect.width + 12,
      lastRect.y + lastRect.height / 2
    );
    await page.mouse.down();
    await page.mouse.move(
      secondLast.x + secondLast.width / 2,
      secondLast.y + secondLast.height / 2,
      { steps: 5 }
    );
    await page.mouse.up();
    await expect(chips.nth(count - 1)).toHaveAttribute("aria-selected", "true");
    await expect(chips.nth(count - 2)).toHaveAttribute("aria-selected", "true");
    await expect(chips.nth(count - 3)).toHaveAttribute(
      "aria-selected",
      "false"
    );
    await expect(members.getByRole("combobox")).toHaveAttribute(
      "aria-expanded",
      "false"
    );

    // From the padding left of the first chip to the middle of the second.
    await page.mouse.move(boxRect.x + 3, first.y + first.height / 2);
    await page.mouse.down();
    await page.mouse.move(
      second.x + second.width / 2,
      second.y + second.height / 2,
      {
        steps: 5,
      }
    );
    await page.mouse.up();
    await expect(chips.nth(0)).toHaveAttribute("aria-selected", "true");
    await expect(chips.nth(1)).toHaveAttribute("aria-selected", "true");
    await expect(chips.nth(2)).toHaveAttribute("aria-selected", "false");

    await page.keyboard.press("ControlOrMeta+C");
    await goto(page, "/teamCal/new");
    const fullUsers = page.getByRole("listbox", {
      name: `${t("plugins.teamcal.fullAccess")} (${t("plugins.teamcal.access.users")})`,
      exact: true,
    });
    await fullUsers.focus();
    await page.keyboard.press("ControlOrMeta+V");
    await expect(fullUsers.getByRole("option")).toHaveCount(2);
  });
});
