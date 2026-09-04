import type { Page, Request, Route } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat, type UserFormat } from "./fixtures/format";
import type {
  MassUpdateFieldMeta,
  MassUpdateParameter,
  MultiSelectMeta,
} from "../lib/rs/multi-select";
import type { MassUpdateMode } from "../components/shared/list/mass-update-mode";

/**
 * The four replacement modes of a mass update field — set, append, search-&-replace and delete — as
 * they reach the backend: which flags and which value each one puts into the [MassUpdateParameter]
 * the page posts to `{page}/update` (see mass-update-mode.ts, `paramForMode`).
 *
 * Driven through the real form (the mode dropdown, the value control, the "replace by" input and the
 * confirm dialog), on the timesheet mass update page — but against a *mocked* backend: `{page}/meta`
 * is fulfilled with one synthetic text field that offers all four modes, and `{page}/update` is
 * intercepted so its body can be asserted and answered with a success, never run. The live database
 * of the sibling selection specs has no undo for a mass update beyond its Excel protocol, and these
 * cases would otherwise write to every ticked entry; here nothing is ticked and nothing is written.
 *
 * The field set is mocked rather than reached through a real selection because the *modes* are the
 * frontend's own logic (`availableModes`/`paramForMode`) and a real `meta` only decides which of them
 * a given field offers — which the timesheet field declarations already cover on the backend side.
 */

/** The timesheet mass update endpoint and route — see TIMESHEET_PAGE.massUpdate. */
const ENDPOINT = "timesheetSelected";
const ROUTE = "/timesheet/mass-update";

/** The one field every case works with: a textarea offering set, append, replace and delete. */
const TEXT_FIELD: MassUpdateFieldMeta = {
  field: "description",
  valueProperty: "textValue",
  label: "E2E description",
  dataType: "STRING",
  maxLength: 4000,
  rows: 3,
  deleteOption: true,
  replaceOption: true,
  appendOption: true,
  appendPreset: false,
};

test.describe.configure({ timeout: 120_000 });

test.describe("mass update replacement modes", () => {
  test("set overwrites the field with the value", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await openForm(page, [TEXT_FIELD]);

    // "Set / overwrite" is the default, so nothing is chosen — only typed.
    await valueInput(page).fill("A brand new note");
    const body = await runAndCapture(page, format);

    expect(body).toEqual({ description: { textValue: "A brand new note" } });
  });

  test("append adds the value to the existing text", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await openForm(page, [TEXT_FIELD]);

    await chooseMode(page, format, "append");
    await valueInput(page).fill(" (added)");
    const body = await runAndCapture(page, format);

    // `append: true` and the value, but no `delete` and no `replaceText` — the flags are mutually
    // exclusive by construction, which is the whole reason the mode is a dropdown.
    expect(body).toEqual({
      description: { append: true, textValue: " (added)" },
    });
  });

  test("search & replace posts the search value and the replacement", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await openForm(page, [TEXT_FIELD]);

    await chooseMode(page, format, "replace");
    // The field's own value control carries what to look for; the extra "replace by" input carries
    // what to put in its place (`MassUpdateParameter.replaceText`).
    await valueInput(page).fill("old wording");
    await replaceByInput(page, format).fill("new wording");
    const body = await runAndCapture(page, format);

    expect(body).toEqual({
      description: { textValue: "old wording", replaceText: "new wording" },
    });
  });

  test("delete clears the field", async ({ loggedInPage: page }) => {
    const format = await userFormat(page);
    await openForm(page, [TEXT_FIELD]);

    await chooseMode(page, format, "delete");
    const body = await runAndCapture(page, format);

    // No value: the whole field is cleared on every selected entry.
    expect(body).toEqual({ description: { delete: true } });
  });

  test("delete with a search text removes only its occurrences", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await openForm(page, [TEXT_FIELD]);

    await chooseMode(page, format, "delete");
    // A field that offers replacing also offers deleting only a substring — the value control stays,
    // now meaning "which occurrences" (see MassUpdateField, the delete branch).
    await valueInput(page).fill("draft");
    const body = await runAndCapture(page, format);

    expect(body).toEqual({ description: { delete: true, textValue: "draft" } });
  });

  test("switching mode keeps what was already typed", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await openForm(page, [TEXT_FIELD]);

    // Typed under "set", then the user changes their mind — the text must not be lost, only the flag
    // added (`paramForMode` carries the value across).
    await valueInput(page).fill("carried over");
    await chooseMode(page, format, "append");
    await expect(valueInput(page)).toHaveValue("carried over");
    const body = await runAndCapture(page, format);

    expect(body).toEqual({
      description: { append: true, textValue: "carried over" },
    });
  });

  test("a field preset to append starts in append mode", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    // The backend can preset a text field to "append" (`showAppendOption = true`, e.g. the incoming
    // invoice's remark) — the form must open in that mode, not in "set".
    await openForm(page, [{ ...TEXT_FIELD, appendPreset: true }]);

    await expect(modeSelect(page, format)).toHaveText(
      modeLabel(format, "append")
    );
    await valueInput(page).fill("appended remark");
    const body = await runAndCapture(page, format);

    expect(body).toEqual({
      description: { append: true, textValue: "appended remark" },
    });
  });

  test("the confirm dialog lists what the server would change", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await openForm(page, [TEXT_FIELD]);

    // The dialog shows the server's preview, not the raw params: an enum's label, a formatted date —
    // here the field's label and a plain value, both taken verbatim from the `preview` answer.
    await page.route(`**/rs/${ENDPOINT}/preview`, (route: Route) =>
      route.fulfill({
        json: {
          selectedCount: 3,
          changes: [
            {
              field: "description",
              label: TEXT_FIELD.label,
              action: "REPLACE",
              value: "old wording",
              replaceValue: "new wording",
            },
          ],
        },
      })
    );

    await valueInput(page).fill("old wording");
    await page.getByRole("button", { name: format.t("save") }).click();

    const dialog = page.getByRole("alertdialog");
    await expect(dialog).toBeVisible();
    await expect(dialog).toContainText(String(TEXT_FIELD.label));
    await expect(dialog).toContainText("old wording");
    await expect(dialog).toContainText("new wording");
  });

  test("a rejected preview shows the error and keeps the dialog closed", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await openForm(page, [TEXT_FIELD]);

    // The preview runs the same check the update does, so a combination it rejects (here a made-up
    // one) surfaces before the write — in the alert, not the dialog.
    await page.route(`**/rs/${ENDPOINT}/preview`, (route: Route) =>
      route.fulfill({
        status: 406,
        json: { validationErrors: [{ message: "Conflicting actions chosen." }] },
      })
    );

    await valueInput(page).fill("whatever");
    await page.getByRole("button", { name: format.t("save") }).click();

    await expect(page.getByText("Conflicting actions chosen.")).toBeVisible();
    await expect(page.getByRole("alertdialog")).toHaveCount(0);
  });
});

/**
 * Answers `{page}/meta` with a synthetic field set and opens the mass update page.
 *
 * The route is registered before the navigation, so the page's first (and only, `staleTime: 0`) meta
 * fetch already gets the mock — the real endpoint would need a selection registered in the session,
 * which these cases deliberately don't create.
 */
async function openForm(
  page: Page,
  fields: MassUpdateFieldMeta[]
): Promise<void> {
  const meta: MultiSelectMeta = {
    title: "E2E mass update",
    // Non-zero so the form's Save button is enabled (`canSubmit`); nothing is really selected.
    selectedCount: 3,
    registeredCount: 3,
    fields,
    listPage: "/timesheet",
    maxMassUpdate: 100,
  };
  await page.route(`**/rs/${ENDPOINT}/meta`, (route) =>
    route.fulfill({ json: meta })
  );
  await goto(page, ROUTE);
  await expect(valueInput(page)).toBeVisible({ timeout: 60_000 });
}

/**
 * Confirms the update and answers the body it posted.
 *
 * `{page}/update` is fulfilled with a success so the real update never runs; the request is awaited
 * rather than the response, because its posted map is what every case asserts on.
 */
async function runAndCapture(
  page: Page,
  format: UserFormat
): Promise<Record<string, MassUpdateParameter>> {
  // Save now asks `{page}/preview` first — the server decides what the dialog lists (see preview()).
  // Answered with one change so the confirm dialog opens; the posted `/update` body, which every case
  // asserts on, comes from the form's own state and is untouched by this answer.
  await page.route(`**/rs/${ENDPOINT}/preview`, (route: Route) =>
    route.fulfill({
      json: {
        selectedCount: 3,
        changes: [
          {
            field: TEXT_FIELD.field,
            label: TEXT_FIELD.label,
            action: "SET",
            value: "preview",
          },
        ],
      },
    })
  );
  await page.route(`**/rs/${ENDPOINT}/update`, (route: Route) =>
    route.fulfill({
      json: {
        modifiedCounter: 3,
        unmodifiedCounter: 0,
        errorCounter: 0,
        resultMessage: "ok",
        errors: [],
        changedFields: [],
      },
    })
  );
  const posted = page.waitForRequest(
    (request: Request) =>
      request.url().endsWith(`/rs/${ENDPOINT}/update`) &&
      request.method() === "POST"
  );

  await page.getByRole("button", { name: format.t("save") }).click();
  // The write is guarded by a confirm dialog — every ticked entry changes at once, so a stray Enter
  // must not be the way past it.
  await page
    .getByRole("alertdialog")
    .getByRole("button", { name: format.t("save") })
    .click();

  return (await posted).postDataJSON() as Record<string, MassUpdateParameter>;
}

/** The field's value control — a textarea labelled by the field (see MassUpdateValueControl). */
function valueInput(page: Page) {
  return page.getByRole("textbox", { name: TEXT_FIELD.label });
}

/** The extra "replace by" input the search-&-replace mode adds (`massUpdate.field.replace`). */
function replaceByInput(page: Page, format: UserFormat) {
  return page.getByRole("textbox", {
    name: label(format, "massUpdate.field.replace"),
  });
}

/** The action dropdown, matched by its label `massUpdate.mode.label` ("Action"). */
function modeSelect(page: Page, format: UserFormat) {
  return page.getByRole("combobox", {
    name: format.t("massUpdate.mode.label"),
  });
}

/** The translated option text of a mode, e.g. "Search & replace" — `delete` has a `._` leaf. */
function modeLabel(format: UserFormat, mode: MassUpdateMode): string {
  return label(format, `massUpdate.mode.${mode}`);
}

/** Opens the action dropdown and picks a mode by its translated option. */
async function chooseMode(
  page: Page,
  format: UserFormat,
  mode: MassUpdateMode
): Promise<void> {
  await modeSelect(page, format).click();
  await page.getByRole("option", { name: modeLabel(format, mode) }).click();
}
