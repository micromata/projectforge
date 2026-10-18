import { test, expect, goto, login } from "./fixtures/auth";
import { hasRole } from "./fixtures/credentials";
import { label, userFormat } from "./fixtures/format";

/**
 * An inactive entry already set stays choosable for everybody (see EntitySearchList.current), while the
 * pickers hide inactive entries otherwise — for a normal user without any way to show them, since
 * `showInactive` is honoured only for HR, finance, orga and admin (AutoCompletion.SHOW_INACTIVE_PERSONS_GROUPS).
 * Without the first half, a normal user could no longer edit an object referencing a user who left.
 *
 * The database is a copy of production, so nothing is written and no row is named: the deactivated user
 * is looked up at run time, and the object referencing it is a new timesheet whose form data
 * (`timesheet/newEntry`) gets that user put in. The lookup of the picker is the real backend's.
 */
test.describe.configure({ timeout: 120_000 });

const NEXT = { "X-PF-Frontend": "next" };

interface Ref {
  id: number;
  displayName: string;
  deactivated?: boolean;
}

test.describe("inactive reference", { tag: "@parallel" }, () => {
  test.skip(
    !hasRole("normalo-user"),
    "no normalo-user in this instance's testAccounts.txt"
  );

  test("keeps a deactivated user already set choosable for a normal user", async ({
    page,
    seedRequest,
  }) => {
    // The full-access account may ask for the deactivated ones.
    const all = (await (
      await seedRequest.get(
        "/rs/user/autosearch?showInactive=true&search=&maxResults=100000",
        { headers: NEXT }
      )
    ).json()) as Ref[];
    const inactive = all.find((user) => user.deactivated && user.displayName);
    test.skip(!inactive, "no deactivated user on this instance");
    const word = inactive!.displayName.split(/\s+/)[0];

    await login(page, "normalo-user");
    // The backend never offers it to a normal user, whatever the request asks for.
    const offered = (await (
      await page.request.get(
        `/rs/user/autosearch?showInactive=true&search=${encodeURIComponent(word)}&maxResults=100000`,
        { headers: NEXT }
      )
    ).json()) as Ref[];
    expect(offered.some((user) => user.id === inactive!.id)).toBe(false);

    await page.route("**/rs/timesheet/newEntry*", async (route) => {
      const response = await route.fetch();
      const json = await response.json();
      json.user = {
        id: inactive!.id,
        displayName: inactive!.displayName,
        deactivated: true,
      };
      await route.fulfill({ response, json });
    });
    const format = await userFormat(page);
    await goto(page, "/timesheet/new");

    const picker = page.getByRole("combobox", {
      name: label(format, "user"),
      exact: true,
    });
    await expect(picker).toContainText(inactive!.displayName);
    await picker.click();
    const popover = page.locator('[data-slot="popover-content"]');
    const option = popover.getByRole("option", {
      name: inactive!.displayName,
      exact: true,
    });
    // Offered on the empty term …
    await expect(option).toBeVisible();
    // … with no checkbox to show the other inactive ones …
    await expect(
      popover.getByRole("checkbox", { name: format.t("filter.showInactive") })
    ).toHaveCount(0);
    // … and still for a term that is part of its name.
    await popover.getByRole("combobox").fill(word);
    await expect(option).toBeVisible();
    // Picking it again keeps it.
    await option.click();
    await expect(picker).toContainText(inactive!.displayName);
  });
});
