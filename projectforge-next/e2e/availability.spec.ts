import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat } from "./fixtures/format";
import { formatDateInput } from "../lib/date-parse";
import { AVAILABILITY_METADATA } from "../lib/metadata/availability.generated";
import { AVAILABILITY_PAGE } from "../components/features/availability/availability.page";
import { columnHeaderKeyOf, columnIdOf } from "../lib/page-def/define-page";

/**
 * The availability entries against the live backend (see AVAILABILITY_PAGE and AvailabilityEntityRest). Nothing
 * is written: the form test posts a period ending before it begins, which the server refuses before any write
 * (AvailabilityEntityRest.validate).
 */
test.describe("availability", () => {
  test("shows the declared columns under the labels of AvailabilityDO", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/availability");

    await expect(
      page.getByRole("heading", {
        name: format.t(AVAILABILITY_PAGE.titleKey),
      })
    ).toBeVisible();
    for (const column of AVAILABILITY_PAGE.columns) {
      const key = columnHeaderKeyOf(column, AVAILABILITY_METADATA);
      await expect(
        page.getByRole("columnheader", {
          name: startsWith(label(format, key)),
        }),
        `column ${columnIdOf(column)}`
      ).toHaveCount(1);
    }
  });

  test("offers the configured types and the internal comment, and refuses an end before the start", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { t } = format;
    const newEntry = async (query = "") =>
      (await (
        await page.request.get(`/rs/availability/newEntry${query}`, {
          headers: { "X-PF-Frontend": "next" },
        })
      ).json()) as {
        user?: { id: number; displayName?: string };
        availableTypes?: string[];
      };
    // The own user — also without an employee record (a freelancer), which the full access user may lack.
    const preset = await newEntry();
    expect(preset.user, "preset with the logged-in user").toBeTruthy();

    await goto(page, "/availability/new");
    await expect(
      page.getByText(preset.user!.displayName!).first()
    ).toBeVisible();
    // The user (and HR staff) get the internal comment.
    await expect(
      page.getByLabel(label(format, "availability.internalComment"), {
        exact: true,
      })
    ).toBeVisible();

    // The configured types.
    const types = preset.availableTypes ?? [];
    expect(types.length).toBeGreaterThan(0);
    await page
      .getByRole("combobox", { name: label(format, "availability.type") })
      .click();
    for (const type of types) {
      await expect(page.getByRole("option", { name: type })).toBeVisible();
    }
    await page.getByRole("option", { name: types[0] }).click();

    const year = new Date().getFullYear() + 3;
    const start = page.getByLabel(label(format, "availability.startDate"), {
      exact: true,
    });
    const end = page.getByLabel(label(format, "availability.endDate"), {
      exact: true,
    });
    await start.fill(formatDateInput(`${year}-02-10`, format.context));
    await start.press("Enter");
    await end.fill(formatDateInput(`${year}-02-09`, format.context));
    await end.press("Enter");

    const saved = page.waitForResponse("**/rs/availability/saveorupdate*");
    await page.getByRole("button", { name: t("save") }).click();
    // Refused by the server before any write (AvailabilityEntityRest.validate), at the end date. The message is
    // translated by the server (it's not in the frontend's catalog), so it's taken from the answer.
    const response = await saved;
    expect(response.status()).toBe(406);
    const error = (
      (await response.json()) as {
        validationErrors: { fieldId?: string; message: string }[];
      }
    ).validationErrors.find((e) => e.fieldId === "endDate");
    expect(error, "a validation error at the end date").toBeTruthy();
    await expect(
      page.getByRole("alert").getByText(error!.message)
    ).toBeVisible();
    // Still on the form: nothing saved.
    await expect(page).toHaveURL(/\/availability\/new/);
  });
});

/** Matches a name beginning with the given text as a whole word. */
function startsWith(text: string): RegExp {
  return new RegExp(`^${text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")}(\\s|$)`);
}
