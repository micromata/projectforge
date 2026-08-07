import { test, expect, goto } from "./fixtures/auth";

/**
 * The book edit page against the live backend.
 *
 * Book 316163 ("Selenium. Web-Applikationen automatisiert testen", signature WT-53) is existing
 * demo data whose REST answer covers the interesting cases at once: `status` and `type` are set,
 * while `editor` and `lendOutComment` are null and therefore *absent* from the JSON
 * (`JsonInclude.Include.NON_NULL`). All assertions are read-only — nothing is saved.
 */
const BOOK_ID = 316163;

test.describe("book edit", () => {
  test("shows the stored status and type in their selects", async ({
    loggedInPage: page,
  }) => {
    await goto(page, `/books/${BOOK_ID}`);

    // The type select renders in the always-present general section.
    const type = page.getByRole("combobox", { name: /typ/i });
    await expect(type).toContainText("Buch");

    // The regression this guards: `status` is the one select whose loaded value differs from the
    // form's default, so it is the only one where the value changes while the dropdown is closed —
    // which used to make Radix's hidden native select bounce an empty value back and wipe the field
    // (see SelectField in book-edit-fields.tsx).
    const status = page.getByRole("combobox", { name: /^status/i });
    await expect(status).toContainText("entsorgt");
  });

  test("labels the fields the way BookDO does", async ({
    loggedInPage: page,
  }) => {
    await goto(page, `/books/${BOOK_ID}`);
    await expect(page.getByRole("textbox", { name: /titel/i })).toHaveValue(
      /Selenium/
    );

    // These four used to carry invented texts ("Auflage", "Bemerkung zur Ausleihe", "Interne
    // Notizen", "Beschreibung"). Each label now comes from BookDO's @PropertyInfo.
    for (const label of [
      /herausgeber:in/i,
      /ausleihnotiz/i,
      /^bemerkung$/i,
      /zusammenfassung/i,
    ]) {
      await expect(
        page.getByRole("textbox", { name: label }),
        `label ${label}`
      ).toHaveCount(1);
    }
    // …and the invented ones are gone.
    await expect(page.getByText(/nur für administratoren/i)).toHaveCount(0);
  });

  test("reports no validation error for fields the backend omitted", async ({
    loggedInPage: page,
  }) => {
    await goto(page, `/books/${BOOK_ID}`);
    // Wait for the loaded data, so the assertion can't pass on an empty form.
    await expect(page.getByRole("textbox", { name: /titel/i })).toHaveValue(
      /Selenium/
    );

    // `editor` and `lendOutComment` are missing from the response. Zod's own English messages must
    // never surface: they are untranslated and, for an optional empty field, plain wrong.
    await expect(page.getByText(/Invalid input/i)).toHaveCount(0);
    await expect(page.getByText(/expected string/i)).toHaveCount(0);
  });
});
