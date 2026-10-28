import { describe, expect, it } from "vitest";
import {
  baseFieldOf,
  blockField,
  blockOf,
  changedFields,
  currentValueOf,
  fieldsFromComparison,
  fieldsToApply,
  initialSelection,
  isNameMismatch,
  selectAll,
  targetOf,
} from "./text-parser-model";

describe("address blocks", () => {
  it("tells the block of a field and its base field", () => {
    expect(blockOf("city")).toBe("business");
    expect(blockOf("postalZipCode")).toBe("postal");
    expect(blockOf("privateAddressText2")).toBe("private");
    expect(blockOf("privatePhone")).toBeNull();
    expect(blockOf("email")).toBeNull();
    expect(baseFieldOf("postalZipCode")).toBe("zipCode");
    expect(baseFieldOf("privateAddressText2")).toBe("addressText2");
    // Not a block field, though it starts like one.
    expect(baseFieldOf("privateEmail")).toBe("privateEmail");
    expect(blockField("city", "private")).toBe("privateCity");
  });

  it("moves a block field to the block the user picked, other fields to their mapping", () => {
    expect(targetOf("city", {}, { business: "postal" })).toBe("postalCity");
    expect(targetOf("privateCity", {}, { business: "postal" })).toBe(
      "privateCity"
    );
    expect(
      targetOf("businessPhone", { businessPhone: "mobilePhone" }, {})
    ).toBe("mobilePhone");
    expect(targetOf("name", {}, {})).toBe("name");
  });
});

describe("offers", () => {
  it("offers only what would change the form", () => {
    const offered = changedFields(
      {
        name: { value: "Muster" },
        city: { value: " Kassel " },
        fax: { value: "" },
      },
      { name: "Muster", city: "Berlin" }
    );
    expect(Object.keys(offered)).toEqual(["city"]);
  });

  it("offers the changes and removals of a vCard, comparing the language with the stored one", () => {
    const offered = fieldsFromComparison(
      {
        name: { vcf: "Muster", db: "Muster" },
        email: { vcf: "", db: "old@example.org" },
        city: { vcf: "Kassel", db: null },
        communicationLanguage: { vcf: "German", db: "German" },
      },
      {
        name: "Muster",
        email: "old@example.org",
        city: null,
        communicationLanguage: "de",
      }
    );
    expect(Object.keys(offered).sort()).toEqual(["city", "email"]);
    expect(offered.email.value).toBe("");
    // A removal is offered, but not ticked in advance.
    expect(initialSelection(offered, {})).toEqual({ city: true, email: false });
  });

  it("names the language as the vCard does, compared with the stored one", () => {
    const offered = fieldsFromComparison(
      { communicationLanguage: { vcf: "English", db: "German" } },
      { communicationLanguage: "de" }
    );
    expect(offered.communicationLanguage.currentValue).toBe("German");
  });

  it("shows the value of the field an offer lands in", () => {
    const values = {
      businessPhone: "1",
      mobilePhone: "2",
      city: "Kassel",
      privateCity: "Berlin",
    };
    const phone = { value: "3" };
    expect(currentValueOf("businessPhone", phone, values, {}, {})).toBe("1");
    expect(
      currentValueOf(
        "businessPhone",
        phone,
        values,
        { businessPhone: "mobilePhone" },
        {}
      )
    ).toBe("2");
    expect(
      currentValueOf(
        "city",
        { value: "Bonn" },
        values,
        {},
        { business: "private" }
      )
    ).toBe("Berlin");
    expect(
      currentValueOf(
        "communicationLanguage",
        { value: "English", currentValue: "German" },
        { communicationLanguage: "de" },
        {},
        {}
      )
    ).toBe("German");
  });

  it("warns about a name differing from the one of a stored address", () => {
    const offer = { value: "Other" };
    expect(isNameMismatch("name", offer, "Muster", true)).toBe(true);
    expect(isNameMismatch("name", offer, "Muster", false)).toBe(false);
    expect(isNameMismatch("name", offer, "", true)).toBe(false);
    expect(isNameMismatch("name", { value: "Muster" }, "Muster", true)).toBe(
      false
    );
    expect(isNameMismatch("city", offer, "Kassel", true)).toBe(false);
  });

  it("ticks everything but a name the form has already", () => {
    const offered = {
      name: { value: "Other" },
      firstName: { value: "Ina" },
      city: { value: "Kassel" },
    };
    expect(
      initialSelection(offered, { name: "Muster", firstName: null })
    ).toEqual({
      name: false,
      firstName: true,
      city: true,
    });
    expect(selectAll(offered, { name: "Muster" }, { name: false })).toEqual({
      name: false,
      firstName: true,
      city: true,
    });
  });

  it("applies the ticked fields to where they land", () => {
    expect(
      fieldsToApply(
        {
          city: { value: "Kassel" },
          zipCode: { value: "34117" },
          email: { value: "a@b.de" },
          fax: { value: "1" },
        },
        { city: true, zipCode: true, email: true, fax: false },
        { email: "privateEmail" },
        { business: "private" }
      )
    ).toEqual({
      privateCity: "Kassel",
      privateZipCode: "34117",
      privateEmail: "a@b.de",
    });
  });
});
