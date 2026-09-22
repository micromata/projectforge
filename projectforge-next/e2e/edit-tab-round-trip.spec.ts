import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat } from "./fixtures/format";

/**
 * What is being filled in survives a look at the change history and back.
 *
 * The bug this guards: the history used to be a route of its own, so opening it unmounted the form
 * and coming back re-initialised it from the server — every entry gone, without a word. The tabs live
 * in one route now and the form is hidden rather than unmounted (see EditPageShell), which is also
 * why the reset in `useEntityEditForm` is guarded by the dirty state: React re-runs the effects of a
 * hidden tree on the way back to visible, so an unguarded reset would throw the entries away one
 * layer later.
 *
 * Asserted on a *book*, the entity whose form is cheapest to fill in — nothing here is book-specific,
 * `EntityEditPage` is the one code path all four hand-built entities take.
 *
 * Nothing is saved: the form is filled in, the tab is visited, and the page is left.
 */
test.describe("edit page tab round trip", () => {
  test("keeps what was entered when the history tab is visited", async ({
    loggedInPage: page,
    seededBook,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, `/book/${seededBook.id}`);

    const title = page.getByRole("textbox", { name: /titel/i });
    await expect(title).toHaveValue(seededBook.title);
    const entered = `${seededBook.title} — unsaved`;
    await title.fill(entered);

    // The save button becoming available is what says the form registered the change — asserted
    // before the detour, so a failure after it can only be about the detour.
    const save = page.getByRole("button", { name: t("save") });
    await expect(save).toBeEnabled();

    const historyTab = page.getByRole("tab", {
      name: t("label.historyOfChanges"),
    });
    await historyTab.click();
    await expect(historyTab).toHaveAttribute("aria-selected", "true");
    // The history is there, so this really is the tab and not merely a URL change. `getByRole`
    // throughout this spec on purpose: a hidden `<Activity>` stays in the DOM with `display: none`,
    // and only the role selectors filter by visibility.
    await expect(
      page.getByRole("listitem").first().getByRole("button")
    ).toBeVisible();
    // And the form is gone from view while its tab is closed.
    await expect(title).toBeHidden();

    // Back to the form — through the first tab, the way a user returns.
    await page.getByRole("tab").first().click();
    await expect(title).toBeVisible();
    await expect(
      title,
      "the entered title must survive the visit to the history"
    ).toHaveValue(entered);
    await expect(
      save,
      "the form must still count as changed after coming back"
    ).toBeEnabled();
  });

  test("comes back to the form from a tab the url arrived on", async ({
    loggedInPage: page,
    seededBook,
  }) => {
    // The case above starts on the form, so the tab it returns to was already rendered once. This one
    // arrives *at the tab* — which is what a bookmark of the old history url does, since
    // EntityTabRedirect turns it into exactly this address.
    //
    // It is the harder case and it was broken: closing the tab is a change of one search parameter,
    // and `router.push` does not commit that on a deep link of the static export — the route was
    // prerendered under a placeholder id, so the push fetched the payload and then put the url back.
    // The form never reappeared and the tab strip was stuck (see EditPageShell, which uses the native
    // History API for it now).
    const { t } = await userFormat(page);
    await goto(page, `/book/${seededBook.id}?tab=history`);
    await expect(
      page.getByRole("listitem").first().getByRole("button")
    ).toBeVisible();

    await page.getByRole("tab").first().click();
    const title = page.getByRole("textbox", { name: /titel/i });
    await expect(title).toBeVisible();
    await expect(title).toHaveValue(seededBook.title);
    // And the parameter is gone from the url, so a reload lands on the form as well.
    await expect(page).not.toHaveURL(/tab=/);
    // The strip agrees: the history is no longer the selected tab.
    await expect(
      page.getByRole("tab", { name: t("label.historyOfChanges") })
    ).toHaveAttribute("aria-selected", "false");
  });

  test("asks for the history only once its tab is opened", async ({
    loggedInPage: page,
    seededBook,
  }) => {
    // The property the history was given a route of its own for: building it is expensive on the
    // server, so a form that merely *has* a history tab must not request one. Kept by rendering the
    // panel only while its tab is open (see EditPageShell) — the tab strip alone costs nothing.
    const { t } = await userFormat(page);
    const requests: string[] = [];
    page.on("request", (request) => {
      const { pathname } = new URL(request.url());
      if (pathname.includes("/history/")) requests.push(pathname);
    });

    await goto(page, `/book/${seededBook.id}`);
    await expect(page.getByRole("textbox", { name: /titel/i })).toHaveValue(
      seededBook.title
    );
    expect(requests, "the form must not fetch the history").toHaveLength(0);

    await page.getByRole("tab", { name: t("label.historyOfChanges") }).click();
    await expect(
      page.getByRole("listitem").first().getByRole("button")
    ).toBeVisible();
    expect(requests).toContain(`/rs/book/history/${seededBook.id}`);
  });

  test("offers the order's forecast as a tab of its form", async ({
    loggedInPage: page,
    seededOrder,
  }) => {
    // The second kind of tab beside the form: declared by the entity rather than derived from its
    // history flag (see ExtraTabDef). Read-only, like the analysis itself.
    const format = await userFormat(page);
    const { t } = format;
    await goto(page, `/order/${seededOrder.id}`);
    const title = page.getByRole("textbox", {
      name: label(format, "fibu.auftrag.title"),
      exact: true,
    });
    await expect(title).toHaveValue(seededOrder.title);

    // `._`: "fibu.auftrag.forecast" is a text of its own *and* the parent of `…forecast.analysis.*`,
    // which the generator can only export as a nested object plus a `_` leaf (see leafKeyOf).
    const forecast = page.getByRole("tab", {
      name: t("fibu.auftrag.forecast._"),
    });
    await forecast.click();
    await expect(forecast).toHaveAttribute("aria-selected", "true");
    await expect(title).toBeHidden();
    // The analysis is the backend's own HTML export, and it says up front that it is computed over
    // the saved order.
    await expect(
      page.getByText(t("order.forecast.savedOnlyHint"))
    ).toBeVisible();

    await page.getByRole("tab").first().click();
    await expect(title).toHaveValue(seededOrder.title);
  });

  test("takes the stored values again once the entry is reopened", async ({
    loggedInPage: page,
    seededBook,
  }) => {
    // The counter-test to the one above: what is kept is kept for *this* visit to the form, not
    // beyond it. A guard that outlived the page would show a stale entry as though it were stored.
    const { t } = await userFormat(page);
    await goto(page, `/book/${seededBook.id}`);
    const title = page.getByRole("textbox", { name: /titel/i });
    await expect(title).toHaveValue(seededBook.title);
    await title.fill(`${seededBook.title} — abandoned`);

    // Cancel, the ordinary way out. It asks nothing: the button *is* the answer (see
    // useUnsavedChangesWarning).
    await page.getByRole("button", { name: t("cancel") }).click();
    await goto(page, `/book/${seededBook.id}`);
    await expect(title).toHaveValue(seededBook.title);
  });
});
