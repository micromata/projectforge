import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";
import { BOOK_METADATA } from "../lib/metadata/book.generated";

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
    await goto(page, `/book/${BOOK_ID}`);

    // The type select renders in the always-present general section.
    const type = page.getByRole("combobox", { name: /typ/i });
    await expect(type).toContainText("Buch");

    // The regression this guards: `status` is the one select whose loaded value differs from the
    // form's default, so it is the only one where the value changes while the dropdown is closed —
    // which used to make Radix's hidden native select bounce an empty value back and wipe the field
    // (see SelectField in components/shared/form/select-field.tsx).
    const status = page.getByRole("combobox", { name: /^status/i });
    await expect(status).toContainText("entsorgt");
  });

  test("labels the fields the way BookDO does", async ({
    loggedInPage: page,
  }) => {
    await goto(page, `/book/${BOOK_ID}`);
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
    await goto(page, `/book/${BOOK_ID}`);
    // Wait for the loaded data, so the assertion can't pass on an empty form.
    await expect(page.getByRole("textbox", { name: /titel/i })).toHaveValue(
      /Selenium/
    );

    // `editor` and `lendOutComment` are missing from the response. Zod's own English messages must
    // never surface: they are untranslated and, for an optional empty field, plain wrong.
    await expect(page.getByText(/Invalid input/i)).toHaveCount(0);
    await expect(page.getByText(/expected string/i)).toHaveCount(0);
  });

  /**
   * The rules below are asserted *against the metadata*, not against numbers written out here: the
   * limit, the mandatory flag and the option lists all come from BookDO, and a test that repeated them
   * would be the very second declaration site this whole mechanism removes. `GenerateNextFieldMetadataTest`
   * guarantees the metadata match the entity, `book.generated.test.ts` states what they say; these
   * tests answer whether the *page* obeys them.
   */
  test.describe("obeys the field rules of BookDO", () => {
    const title = BOOK_METADATA.fields.title;

    test("stops typing at the column's length", async ({
      loggedInPage: page,
    }) => {
      await goto(page, `/book/new`);
      const input = page.getByRole("textbox", { name: /titel/i });
      await input.fill("x".repeat(title.maxLength + 50));
      await expect(input).toHaveValue("x".repeat(title.maxLength));
      // The HTML attribute did it, so the Zod net never had to complain.
      await expect(page.getByText(/zeichen/i)).toHaveCount(0);
    });

    /**
     * Both of the next two try to save: the form validates `onSubmit` (see BookEditForm), so that is
     * where the schema speaks. Nothing is written either way — the schema refuses before the request,
     * which the route assertion proves.
     */
    test("refuses a value that got past the input over the limit", async ({
      loggedInPage: page,
    }) => {
      const { t } = await userFormat(page);
      let saveAttempted = false;
      await page.route("**/rs/book/saveorupdate*", (route) => {
        saveAttempted = true;
        return route.abort();
      });

      await goto(page, `/book/new`);
      const input = page.getByRole("textbox", { name: /titel/i });
      // maxLength only bounds typing and pasting. A programmatic change — an import, a browser
      // extension, a future autofill — goes past it, which is why the schema keeps the rule too.
      // Written through the prototype's setter, not `field.value =`: React tracks the last value it
      // rendered on the node itself, and a plain assignment updates that tracker, so React would
      // consider the following event a no-op and the form would never even turn dirty.
      await input.evaluate(
        (element, value) => {
          const field = element as HTMLInputElement;
          const setter = Object.getOwnPropertyDescriptor(
            HTMLInputElement.prototype,
            "value"
          )?.set;
          setter?.call(field, value);
          field.dispatchEvent(new Event("input", { bubbles: true }));
        },
        "x".repeat(title.maxLength + 1)
      );
      // Blur before saving: a field shows its error once it has been touched (see FieldShell), and a
      // programmatic value change touches nothing.
      await input.blur();
      await page.getByRole("button", { name: t("save") }).click();

      await expect(
        page.getByText(
          t("validation.error.maxLength", {
            arg0: t("book.title._"),
            arg1: title.maxLength,
          })
        )
      ).toBeVisible();
      expect(
        saveAttempted,
        "the too long value must not reach the server"
      ).toBe(false);
    });

    test("demands a title with the backend's own wording", async ({
      loggedInPage: page,
    }) => {
      const { t } = await userFormat(page);
      let saveAttempted = false;
      await page.route("**/rs/book/saveorupdate*", (route) => {
        saveAttempted = true;
        return route.abort();
      });

      await goto(page, `/book/${BOOK_ID}`);
      const input = page.getByRole("textbox", { name: /titel/i });
      await expect(input).toHaveValue(/Selenium/);
      await input.fill("");
      await input.blur();
      await page.getByRole("button", { name: t("save") }).click();

      // The wording is the backend's own `validation.error.fieldRequired`, so a client side complaint
      // reads exactly like the HTTP 406 the server would have answered with.
      await expect(
        page.getByText(
          t("validation.error.fieldRequired", { arg0: t("book.title._") })
        )
      ).toBeVisible();
      expect(saveAttempted, "the book must keep its title").toBe(false);
    });

    test("offers every constant of BookType and BookStatus, labelled by the backend", async ({
      loggedInPage: page,
    }) => {
      const { t } = await userFormat(page);
      await goto(page, `/book/${BOOK_ID}`);
      const type = page.getByRole("combobox", { name: /typ/i });
      await expect(type).toContainText(t("book.type.book"));

      await type.click();
      const options = page.getByRole("option");
      await expect(options).toHaveCount(
        BOOK_METADATA.fields.type.enumValues.length
      );
      for (const value of BOOK_METADATA.fields.type.enumValues) {
        await expect(
          page.getByRole("option", { name: t(value.i18nKey), exact: true }),
          value.value
        ).toHaveCount(1);
      }
      await page.keyboard.press("Escape");

      const status = page.getByRole("combobox", { name: /^status/i });
      await status.click();
      await expect(page.getByRole("option")).toHaveCount(
        BOOK_METADATA.fields.status.enumValues.length
      );
      await page.keyboard.press("Escape");
    });

    test("lets the optional type be cleared and the mandatory status not", async ({
      loggedInPage: page,
    }) => {
      const { t } = await userFormat(page);
      await goto(page, `/book/${BOOK_ID}`);
      await expect(page.getByRole("combobox", { name: /typ/i })).toContainText(
        t("book.type.book")
      );

      // The ✕ next to a select exists exactly where the column is nullable — SelectField defaults
      // `clearable` to `!required` from the metadata, so neither decision is made in the section.
      const clear = (label: string) =>
        page.getByRole("button", {
          name: `${t("reset")}: ${label}`,
        });
      await expect(clear(t("book.type._"))).toHaveCount(1);
      await expect(clear(t("status"))).toHaveCount(0);
    });
  });
});
